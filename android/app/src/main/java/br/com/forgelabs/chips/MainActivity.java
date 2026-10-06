package br.com.forgelabs.chips;

import android.app.Activity;
import android.content.Intent;
import android.content.ClipData;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AtomicFile;
import android.util.Base64;
import android.webkit.*;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

public class MainActivity extends Activity {
 private WebView web;
 private volatile File activeUiDir;
 private volatile boolean uiIsReady=false;
 private int uiGeneration=0;
 private final AtomicBoolean updateRunning=new AtomicBoolean(false);
 private static final String[] UI_FILES={"index.html","app.css","app.js","excel.js","manifest.json","icon.svg"};
 private ValueCallback<Uri[]> chooserCallback;
 private Uri cameraUri;
 private File cameraFile;
 private static final int PICK_FILE=70;
 private static final String HOST="appassets.androidplatform.net";
 @Override public void onCreate(Bundle state){
  super.onCreate(state);web=new WebView(this);setContentView(web);
  web.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
  web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(true);
  web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  web.addJavascriptInterface(new StoreBridge(),"NativeStore");
  web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
  int code=nativeVersion();SharedPreferences settings=getPreferences(MODE_PRIVATE);
  if(settings.getInt("uiNativeVersion",0)!=code)settings.edit().remove("activeUpdate").putInt("uiNativeVersion",code).commit();
  selectInterface();
  WebViewAssetLoader.AssetsPathHandler bundled=new WebViewAssetLoader.AssetsPathHandler(this);
  WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",path->{
   if(!java.util.Arrays.asList(UI_FILES).contains(path))return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
   File current=activeUiDir;
   if(current!=null)try{String mime=path.endsWith(".js")?"application/javascript":path.endsWith(".css")?"text/css":path.endsWith(".svg")?"image/svg+xml":path.endsWith(".json")?"application/json":"text/html";return new WebResourceResponse(mime,"UTF-8",new FileInputStream(new File(current,path)));}catch(IOException e){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
   return bundled.handle(path);
  }).build();
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest req){
    WebResourceResponse local=loader.shouldInterceptRequest(req.getUrl());if(local!=null)return local;
    if("https".equals(req.getUrl().getScheme()) && ("/api/login".equals(req.getUrl().getPath()) || "/api/animals".equals(req.getUrl().getPath())))return null;
    String configured=getPreferences(MODE_PRIVATE).getString("connection","{}");
    try{String url=new JSONObject(configured).optString("url");Uri server=Uri.parse(url);if("https".equals(req.getUrl().getScheme())&&server.getHost()!=null&&server.getHost().equals(req.getUrl().getHost()))return null;}catch(Exception ignored){}
    return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){
    Uri url=req.getUrl();if("https".equals(url.getScheme())&&HOST.equals(url.getHost())&&url.getPath().startsWith("/assets/"))return false;
    if("https".equals(url.getScheme()))try{startActivity(new Intent(Intent.ACTION_VIEW,url));}catch(Exception ignored){}return true;
   }
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params){
    if(chooserCallback!=null)chooserCallback.onReceiveValue(null);chooserCallback=callback;
    if(cameraFile!=null)cameraFile.delete();cameraUri=null;cameraFile=null;
    String accept=String.join(",",params.getAcceptTypes());boolean image=accept.contains("image");
    Intent files=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(image?"image/*":"application/json");
    Intent chooser=Intent.createChooser(files,image?"Foto do animal":"Escolher backup");
    if(image&&params.isCaptureEnabled())try{
     File folder=getExternalFilesDir(Environment.DIRECTORY_PICTURES);cameraFile=File.createTempFile("animal-",".jpg",folder);
     cameraUri=FileProvider.getUriForFile(MainActivity.this,getPackageName()+".files",cameraFile);
     Intent camera=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);camera.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);camera.setClipData(ClipData.newRawUri("foto",cameraUri));
     startActivityForResult(camera,PICK_FILE);return true;
    }catch(Exception e){
     if(cameraFile!=null)cameraFile.delete();cameraUri=null;cameraFile=null;
     chooserCallback=null;callback.onReceiveValue(null);
     Toast.makeText(MainActivity.this,"Não foi possível abrir a câmera. Confira se há um aplicativo de câmera instalado e tente novamente.",Toast.LENGTH_LONG).show();return true;
    }
    try{startActivityForResult(chooser,PICK_FILE);return true;}catch(Exception e){chooserCallback=null;callback.onReceiveValue(null);return false;}
   }
  });
  loadInterface(false);
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PICK_FILE||chooserCallback==null)return;Uri[] values=null;if(result==RESULT_OK){if(cameraUri!=null&&cameraFile!=null&&cameraFile.length()>0)values=new Uri[]{cameraUri};else if(data!=null&&data.getData()!=null)values=new Uri[]{data.getData()};}chooserCallback.onReceiveValue(values);chooserCallback=null;}
 @Override public void onBackPressed(){web.evaluateJavascript("navigate('cad')",null);}
 private String fail(Exception e){return "{\"error\":\"Não foi possível acessar os arquivos do app. Confira o espaço livre.\"}";}
 private int nativeVersion(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionCode;}catch(Exception e){return 0;}}
 private void selectInterface(){String version=getPreferences(MODE_PRIVATE).getString("activeUpdate","");File dir=new File(new File(getFilesDir(),"ui-updates"),version);activeUiDir=null;if(version.matches("[a-f0-9]{64}")){boolean complete=true;for(String name:UI_FILES)if(!new File(dir,name).isFile())complete=false;if(complete)activeUiDir=dir;}}
 private void loadInterface(boolean toSettings){uiIsReady=false;int generation=++uiGeneration;web.loadUrl("https://"+HOST+"/assets/index.html"+(toSettings?"#conexao":""));if(activeUiDir!=null)new Handler(Looper.getMainLooper()).postDelayed(()->{if(generation==uiGeneration&&!uiIsReady){getPreferences(MODE_PRIVATE).edit().remove("activeUpdate").commit();activeUiDir=null;Toast.makeText(this,"A atualização não iniciou. A interface original foi restaurada; seus cadastros foram mantidos.",Toast.LENGTH_LONG).show();loadInterface(true);}},8000);}
 private static String sha256(byte[] bytes)throws Exception{byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder text=new StringBuilder();for(byte b:hash)text.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return text.toString();}
 private byte[] fetchUpdate(String base,String token)throws Exception{
  URL url=new URL(base+"/api/app-update?download=1");HttpsURLConnection request=(HttpsURLConnection)url.openConnection();request.setInstanceFollowRedirects(false);request.setConnectTimeout(15000);request.setReadTimeout(15000);request.setRequestProperty("Authorization","Bearer "+token);request.setRequestProperty("Cache-Control","no-cache");
  try{int status=request.getResponseCode();if(status==401)throw new IOException("Entre novamente em Conexão para atualizar.");if(status!=200)throw new IOException("Atualização indisponível. Confira se o último deploy terminou na Vercel.");try(InputStream in=request.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1){if(out.size()+count>2000000)throw new IOException("Atualização maior que o limite permitido.");out.write(buffer,0,count);}return out.toByteArray();}}finally{request.disconnect();}
 }
 private void reportUpdate(JSONObject result,boolean reload){runOnUiThread(()->{web.evaluateJavascript("onAppUpdate("+result.toString()+")",null);if(reload){selectInterface();loadInterface(true);}});}
 private void removeUpdate(File dir){File[] children=dir.listFiles();if(children!=null)for(File f:children)f.delete();dir.delete();}
 private SecretKey loginKey()throws Exception{KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);String alias="sitio-remembered-login";if(!store.containsAlias(alias)){KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generator.generateKey();}return (SecretKey)store.getKey(alias,null);}
 public class StoreBridge {
  @JavascriptInterface public synchronized String rememberLogin(String json){try{JSONObject login=new JSONObject(json);if(!"https".equals(Uri.parse(login.getString("url")).getScheme())||login.getString("password").length()>512||json.length()>4096)throw new IOException();Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,loginKey());String encrypted=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(json.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);if(!getPreferences(MODE_PRIVATE).edit().putString("rememberedLogin",encrypted).commit())throw new IOException();return "{\"ok\":true}";}catch(Exception e){return "{\"error\":\"Não foi possível lembrar a senha neste aparelho.\"}";}}
  @JavascriptInterface public synchronized String getRememberedLogin(){try{String encrypted=getPreferences(MODE_PRIVATE).getString("rememberedLogin","");if(encrypted.isEmpty())return "{}";String[] parts=encrypted.split(":",2);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,loginKey(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);}catch(Exception e){return "{}";}}
  @JavascriptInterface public synchronized void forgetLogin(){getPreferences(MODE_PRIVATE).edit().remove("rememberedLogin").commit();}

  @JavascriptInterface public void uiReady(){uiIsReady=true;}
  @JavascriptInterface public String getAppVersion(){String active=getPreferences(MODE_PRIVATE).getString("activeUpdate","");return "APK "+nativeVersion()+(active.isEmpty()?" • Interface original":" • Interface atualizada "+active.substring(0,8));}
  @JavascriptInterface public void restoreInterface(){runOnUiThread(()->{getPreferences(MODE_PRIVATE).edit().remove("activeUpdate").commit();activeUiDir=null;loadInterface(true);});}
  @JavascriptInterface public void installUpdate(){if(!updateRunning.compareAndSet(false,true))return;new Thread(()->{
   File stage=null;
   try{
    JSONObject connection=new JSONObject(getPreferences(MODE_PRIVATE).getString("connection","{}"));String base=connection.optString("url"),token=connection.optString("token");Uri origin=Uri.parse(base);if(!"https".equals(origin.getScheme())||origin.getHost()==null||origin.getUserInfo()!=null||origin.getQuery()!=null||origin.getFragment()!=null||!(origin.getPath()==null||origin.getPath().isEmpty()||"/".equals(origin.getPath()))||token.isEmpty())throw new IOException("Configure a conexão HTTPS e entre com sua senha antes de atualizar.");while(base.endsWith("/"))base=base.substring(0,base.length()-1);
    JSONObject bundle=new JSONObject(new String(fetchUpdate(base,token),StandardCharsets.UTF_8));if(!"sitio-ui-v1".equals(bundle.optString("format")))throw new IOException("Formato de atualização incompatível.");
    if(bundle.getInt("minNativeVersion")>nativeVersion()){reportUpdate(new JSONObject().put("message","Esta novidade exige um novo APK. Gere a versão atual pelo GitHub Actions."),false);return;}
    String version=bundle.getString("version");if(!version.matches("[a-f0-9]{64}"))throw new IOException("Versão inválida.");String previous=getPreferences(MODE_PRIVATE).getString("activeUpdate","");if(version.equals(previous)){reportUpdate(new JSONObject().put("message","O aplicativo já está atualizado."),false);return;}
    JSONArray files=bundle.getJSONArray("files");if(files.length()!=UI_FILES.length)throw new IOException("Atualização incompleta.");java.util.Map<String,byte[]> checked=new java.util.HashMap<>();
    for(int i=0;i<files.length();i++){JSONObject item=files.getJSONObject(i);String name=item.getString("name");if(!java.util.Arrays.asList(UI_FILES).contains(name)||checked.containsKey(name))throw new IOException("Arquivo de atualização inválido.");byte[] bytes=Base64.decode(item.getString("data"),Base64.NO_WRAP);if(bytes.length==0||bytes.length>500000||bytes.length!=item.getInt("bytes")||!sha256(bytes).equals(item.getString("sha256")))throw new IOException("A atualização não passou na verificação. Tente novamente.");checked.put(name,bytes);}
    File root=new File(getFilesDir(),"ui-updates");if(!root.exists()&&!root.mkdirs())throw new IOException("Sem espaço para atualizar.");stage=new File(root,version+".pending");if(stage.exists())removeUpdate(stage);if(!stage.mkdirs())throw new IOException("Não foi possível preparar a atualização.");
    for(String name:UI_FILES){try(FileOutputStream out=new FileOutputStream(new File(stage,name))){out.write(checked.get(name));out.getFD().sync();}}
    File target=new File(root,version);if(target.exists())removeUpdate(target);if(!stage.renameTo(target))throw new IOException("Não foi possível concluir a atualização.");stage=null;
    if(!getPreferences(MODE_PRIVATE).edit().putString("activeUpdate",version).putInt("uiNativeVersion",nativeVersion()).commit())throw new IOException("Não foi possível ativar a atualização.");
    File[] older=root.listFiles();if(older!=null)for(File f:older)if(f.isDirectory()&&!f.getName().equals(version)&&!f.getName().equals(previous))removeUpdate(f);
    reportUpdate(new JSONObject().put("message","Atualização instalada. Reabrindo…"),true);
   }catch(Exception e){try{reportUpdate(new JSONObject().put("error",e.getMessage()==null?"Não foi possível atualizar. A versão atual foi mantida.":e.getMessage()),false);}catch(JSONException ignored){}}
   finally{if(stage!=null)removeUpdate(stage);updateRunning.set(false);}
  },"sitio-update").start();}

  @JavascriptInterface public synchronized String readAll(){try{File dir=new File(getFilesDir(),"records");if(!dir.exists()&&!dir.mkdirs())throw new IOException();JSONArray records=new JSONArray();File[] files=dir.listFiles();if(files==null)throw new IOException();java.util.Set<String> names=new java.util.TreeSet<>();for(File f:files){String n=f.getName();if(n.endsWith(".json"))names.add(n);else if(n.endsWith(".json.bak"))names.add(n.substring(0,n.length()-4));}for(String n:names){AtomicFile file=new AtomicFile(new File(dir,n));records.put(new JSONObject(new String(file.readFully(),StandardCharsets.UTF_8)));}return new JSONObject().put("records",records).toString();}catch(Exception e){return fail(e);}}
  @JavascriptInterface public synchronized String saveRecord(String json,boolean replace){FileOutputStream out=null;AtomicFile target=null;try{
   if(json.length()>1500000)throw new IOException();JSONObject record=new JSONObject(json);String chip=record.getString("chip");if(!chip.matches("[0-9]{1,30}"))throw new IOException();
   File dir=new File(getFilesDir(),"records");if(!dir.exists()&&!dir.mkdirs())throw new IOException();File path=new File(dir,chip+".json");if(path.exists()&&!replace)return "{\"error\":\"Este chip já está cadastrado.\"}";
   target=new AtomicFile(path);out=target.startWrite();out.write(json.getBytes(StandardCharsets.UTF_8));target.finishWrite(out);return "{\"ok\":true}";
  }catch(Exception e){if(target!=null&&out!=null)target.failWrite(out);return fail(e);}}
  @JavascriptInterface public String getConnection(){return getPreferences(MODE_PRIVATE).getString("connection","{\"url\":\"\",\"token\":\"\"}");}
  @JavascriptInterface public String setConnection(String json){try{JSONObject data=new JSONObject(json);String url=data.optString("url");if(!url.isEmpty()&&!"https".equals(Uri.parse(url).getScheme()))throw new IOException();if(json.length()>4096)throw new IOException();boolean ok=getPreferences(MODE_PRIVATE).edit().putString("connection",json).commit();if(!ok)throw new IOException();return "{\"ok\":true}";}catch(Exception e){return fail(e);}}
  @JavascriptInterface public String exportFile(String name,String mime,String base64){try{
   if(!name.matches("[a-zA-Z0-9._-]{1,120}")||!mime.matches("[a-zA-Z0-9./+-]+"))throw new IOException();File dir=new File(getCacheDir(),"exports");if(!dir.exists()&&!dir.mkdirs())throw new IOException();File file=new File(dir,name);try(FileOutputStream out=new FileOutputStream(file)){out.write(Base64.decode(base64,Base64.DEFAULT));}
   Uri uri=FileProvider.getUriForFile(MainActivity.this,getPackageName()+".files",file);
   runOnUiThread(()->{try{Intent share=new Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);share.setClipData(ClipData.newRawUri(name,uri));startActivity(Intent.createChooser(share,"Salvar ou compartilhar arquivo"));}catch(Exception e){Toast.makeText(MainActivity.this,"Nenhum aplicativo disponível para salvar/compartilhar.",Toast.LENGTH_LONG).show();}});return "{\"ok\":true}";
  }catch(Exception e){return fail(e);}}
 }
}
