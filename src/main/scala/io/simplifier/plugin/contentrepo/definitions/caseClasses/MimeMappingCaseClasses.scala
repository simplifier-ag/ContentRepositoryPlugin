package io.simplifier.plugin.contentrepo.definitions.caseClasses

import io.simplifier.plugin.contentrepo.dto.RestMessages.RestMessage
import io.simplifier.pluginbase.util.api.ApiMessage
import io.swagger.annotations.ApiModelProperty

object MimeMappingCaseClasses {

  case class MimeMappingAddRequest(@ApiModelProperty(value="The name of the extension.") extension: String,
                                   @ApiModelProperty(value="The name of the mime type.") mimetype: String) extends ApiMessage

  case class MimeMappingAddResponse(@ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                    @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class MimeMappingGetRequest(@ApiModelProperty(value="The extension of this mime mapping.") extension: String) extends ApiMessage

  case class MimeMappingGetResponse(@ApiModelProperty(value="The extension of this mime mapping.") extension: String,
                                    @ApiModelProperty(value="The mime type of this mime mapping.") mimeType: String,
                                    @ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                    @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class MimeMappingListResponseItem(@ApiModelProperty(value="The extension of the mime mapping.") extension: String,
                                         @ApiModelProperty(value="The mime type of of the mime mapping.") mimeType: String) extends ApiMessage

  case class MimeMappingListResponse(@ApiModelProperty(value="The list of queried mime mappings.") mappings: Seq[MimeMappingListResponseItem],
                                     @ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                     @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class MimeMappingEditRequest(@ApiModelProperty(value="The extension of the mime mapping to be updated.") extension: String,
                                    @ApiModelProperty(value="The mime type of of the mime mapping to be updated.") mimetype: String) extends ApiMessage

  case class MimeMappingEditResponse(@ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                     @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class MimeMappingDeleteRequest(@ApiModelProperty(value="The extension of the mime mapping to be deleted.") extension: String) extends ApiMessage

  case class MimeMappingDeleteResponse(@ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                       @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class MimeMappingReturn(@ApiModelProperty(value="The extension of the mime mapping.") extension: String,
                               @ApiModelProperty(value="The name of the mime type.") mimeType:String) extends ApiMessage

}
