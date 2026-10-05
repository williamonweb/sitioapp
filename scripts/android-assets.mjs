import {mkdir,copyFile} from 'node:fs/promises';
const dest=new URL('../android/app/src/main/assets/',import.meta.url);await mkdir(dest,{recursive:true});for(const name of ['index.html','app.css','app.js','excel.js','manifest.json','icon.svg'])await copyFile(new URL('../public/'+name,import.meta.url),new URL(name,dest));console.log('Arquivos do aplicativo atualizados.');
