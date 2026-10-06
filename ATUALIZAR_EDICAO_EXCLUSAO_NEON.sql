-- Preserva os registros existentes. Execute antes de publicar a versão 2.7.
ALTER TABLE animals ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE animals ADD COLUMN IF NOT EXISTS revision INTEGER NOT NULL DEFAULT 1;
