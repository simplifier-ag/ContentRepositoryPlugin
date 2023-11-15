--
-- Add a description to the content file and content repository and increase the description size of the content folder to 512.
--

ALTER TABLE ${prefix}ContentFile
  ADD COLUMN file_description VARCHAR(2048) NOT NULL;

ALTER TABLE ${prefix}ContentRepository
  ADD COLUMN description VARCHAR(2048) NOT NULL;

ALTER TABLE ${prefix}ContentFolder
  MODIFY folder_description VARCHAR(2048) NOT NULL;