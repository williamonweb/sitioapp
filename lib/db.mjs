import { neon } from '@neondatabase/serverless';
export function sql(){if(!process.env.DATABASE_URL)throw new Error('DATABASE_URL não configurada');return neon(process.env.DATABASE_URL)}
