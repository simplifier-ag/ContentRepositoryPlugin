package io.simplifier.plugin.contentrepo.model

import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column

/**
 * Model: Mime Mapping.
 * @author Christian Simon
 */
class MimeMapping(
    @Column("mime_mapping_id") var id: Int,
    var ext: String,
    @Column("mime_type") var mimeType: String)
  extends KeyedEntity[Int]