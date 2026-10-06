-- Preserva todos os animais cadastrados.
ALTER TABLE animals ADD COLUMN IF NOT EXISTS kennel TEXT NOT NULL DEFAULT '';
