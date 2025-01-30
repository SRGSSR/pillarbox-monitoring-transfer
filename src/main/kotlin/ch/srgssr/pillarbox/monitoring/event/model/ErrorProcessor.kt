package ch.srgssr.pillarbox.monitoring.event.model

/**
 * A processor that identifies and categorizes error messages found in an error data node.
 *
 * This processor determines whether an error message corresponds to a known content restriction type
 * and annotates the data node with an appropriate classification.
 */
internal class ErrorProcessor : DataProcessor {
  /**
   * Process only on ERROR events.
   */
  override fun shouldProcess(eventName: String): Boolean = eventName == "ERROR"

  /**
   * Processes the given data node to determine the type of error based on its message:
   *
   * If the message matches a predefined content restriction category, an `error_type` field is added
   * and the error is flagged as a business error.
   *
   * @param data The data node to process.
   *
   * @return The enriched data node with additional error classification.
   */
  override fun process(data: MutableMap<String, Any?>): MutableMap<String, Any?> {
    val type =
      (data["message"] as? String)?.let {
        ContentRestriction.findByMessage(it)
      }

    data["error_type"] = type?.name
    data["business_error"] = type != null

    return data
  }
}

/**
 * Enum representing different content restriction types based on predefined error messages.
 */
internal enum class ContentRestriction(
  val messages: List<String>,
) {
  AGERATING12(
    listOf(
      "To protect children this content is only available between 8PM and 6AM.",
      "Pour protéger les enfants, ce contenu est accessible entre 20h et 6h.",
      "Per proteggere i bambini, questo media è disponibile solo fra le 20 e le 6.",
      "Per proteger uffants, è quest cuntegn disponibel mo tranter las 20.00 e las 06.00.",
    ),
  ),
  AGERATING18(
    listOf(
      "To protect children this content is only available between 10PM and 5AM.",
      "Pour protéger les enfants, ce contenu est accessible entre 23h et 5h.",
      "Per proteggere i bambini, questo media è disponibile solo fra le 23 le 5.",
      "Per proteger uffants, è quest cuntegn disponibel mo tranter las 23.00 e las 05.00.",
    ),
  ),
  COMMERCIAL(
    listOf(
      "This commercial content is not available.",
      "Ce contenu n'est actuellement pas disponible.",
      "Questo contenuto commerciale non è disponibile.",
      "Quest medium commerzial n'è betg disponibel.",
    ),
  ),
  ENDDATE(
    listOf(
      "This content is not available anymore.",
      "Ce contenu n'est plus disponible.",
      "Questo media non è più disponibile.",
      "Quest cuntegn n'è betg pli disponibel.",
    ),
  ),
  GEOBLOCK(
    listOf(
      "This content is not available outside Switzerland.",
      "La RTS ne dispose pas des droits de diffusion en dehors de la Suisse.",
      "Questo media non è disponibile fuori dalla Svizzera.",
      "Quest cuntegn n'è betg disponibel ordaifer la Svizra.",
    ),
  ),
  JOURNALISTIC(
    listOf(
      "This content is temporarily unavailable for journalistic reasons.",
      "Ce contenu est temporairement indisponible pour des raisons éditoriales.",
      "Questo contenuto è temporaneamente non disponibile per motivi editoriali.",
      "Quest cuntegn na stat ad interim betg a disposiziun per motivs publicistics.",
    ),
  ),
  LEGAL(
    listOf(
      "This content is not available due to legal restrictions.",
      "Pour des raisons juridiques, ce contenu n'est pas disponible.",
      "Il contenuto non è fruibile a causa di restrizioni legali.",
      "Quest cuntegn n'è betg disponibel perquai ch'el è scadì.",
    ),
  ),
  STARTDATE(
    listOf(
      "This content is not available yet.",
      "Ce contenu n'est pas encore disponible. Veuillez réessayer plus tard.",
      "Il contenuto non è ancora disponibile. Per cortesia prova più tardi.",
      "Quest cuntegn n'è betg anc disponibel. Empruvai pli tard.",
    ),
  ),
  UNKNOWN(
    listOf(
      "This content is not available.",
      "Ce contenu n'est actuellement pas disponible.",
      "Questo media non è disponibile.",
      "Quest cuntegn n'è betg disponibel.",
    ),
  ),
  ;

  companion object {
    private val messageToTypeMap: Map<String, ContentRestriction> by lazy {
      entries
        .flatMap { type ->
          type.messages.map { message -> message to type }
        }.toMap()
    }

    fun findByMessage(message: String): ContentRestriction? = messageToTypeMap[message]
  }
}
