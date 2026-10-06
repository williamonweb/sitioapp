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
import org.json.*;

public class MainActivity extends Activity {
 private WebView web;
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
  WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
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
  web.loadUrl("https://"+HOST+"/assets/index.html");
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PICK_FILE||chooserCallback==null)return;Uri[] values=null;if(result==RESULT_OK){if(cameraUri!=null&&cameraFile!=null&&cameraFile.length()>0)values=new Uri[]{cameraUri};else if(data!=null&&data.getData()!=null)values=new Uri[]{data.getData()};}chooserCallback.onReceiveValue(values);chooserCallback=null;}
 @Override public void onBackPressed(){web.evaluateJavascript("navigate('cad')",null);}
 private String fail(Exception e){return "{\"error\":\"Não foi possível acessar os arquivos do app. Confira o espaço livre.\"}";}
 public class StoreBridge {
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
