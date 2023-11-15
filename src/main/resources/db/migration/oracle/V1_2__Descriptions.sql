--
-- Add a description to the content file and content repository and increase the description size of the content folder to 512.
--


ALTER TABLE ${prefix}CONTENTFILE
  ADD FILE_DESCRIPTION VARCHAR2(2048 BYTE) DEFAULT 'no description' NOT NULL;

ALTER TABLE ${prefix}CONTENTREPOSITORY
  ADD DESCRIPTION VARCHAR2(2048 BYTE) DEFAULT 'no description' NOT NULL;

ALTER TABLE ${prefix}CONTENTFOLDER
  MODIFY FOLDER_DESCRIPTION VARCHAR2(2048 BYTE);