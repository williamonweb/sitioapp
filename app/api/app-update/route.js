import {authorized,json,preflight} from '../../../lib/auth.mjs';
import {appUpdateBundle} from '../../../lib/app-update.mjs';
export const runtime='nodejs';
export const dynamic='force-dynamic';
export function OPTIONS(req){return preflight(req)}
export async function GET(req){if(!authorized(req))return json(req,{error:'Entre novamente em Conexão para atualizar.'},401);try{return json(req,await appUpdateBundle(undefined,new URL(req.url).searchParams.get('download')==='1'))}catch(e){console.error('Atualização indisponível',e.message);return json(req,{error:'Não foi possível preparar a atualização. Confira o deploy.'},503)}}
