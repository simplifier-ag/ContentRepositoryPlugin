package io.simplifier.plugin.contentrepo.model

/**
 * Trait describing security schemes.
 * @author Christian Simon
 */
sealed trait SecurityScheme {

  val name: String

}

/**
 * Companion object of trait SecurityScheme.
 */
object SecurityScheme {

  /** Private security scheme */
  case object Private extends SecurityScheme {
    override val name = "private"
  }

  /** Public security scheme */
  case object Public extends SecurityScheme {
    override val name = "public"
  }

  private val values = Seq(Private, Public)
  private val valuesByName = values.map { scheme => (scheme.name, scheme) }.toMap

  /**
   * Get security scheme by name.
   */
  def parse(name: String): Option[SecurityScheme] = valuesByName.get(name)

}

