import {readFile} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {join} from 'node:path';
export const updateFiles=['index.html','app.css','app.js','excel.js','manifest.json','icon.svg'];
export async function appUpdateBundle(directory=join(process.cwd(),'public'),download=true){
 const files=await Promise.all(updateFiles.map(async name=>{const bytes=await readFile(/*turbopackIgnore: true*/ join(/*turbopackIgnore: true*/ directory,name));return {name,bytes:bytes.length,sha256:createHash('sha256').update(bytes).digest('hex'),...(download?{data:bytes.toString('base64')}:{})}}));
 const minNativeVersion=9;
 const version=createHash('sha256').update(JSON.stringify([minNativeVersion,files.map(({name,sha256})=>({name,sha256}))])).digest('hex');
 return {format:'sitio-ui-v1',version,label:'2.8',minNativeVersion,files};
}
