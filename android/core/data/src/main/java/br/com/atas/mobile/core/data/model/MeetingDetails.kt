package br.com.atas.mobile.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MeetingDetails(
    val orgao: String = "",
    val frequencia: String = "",
    val tipo: String = "Sacramental",
    val ala: String = "",
    val preside: String = "",
    val dirige: String = "",
    val organista: String = "",
    val regente: String = "",
    val oficiantesSacramento: String = "",
    val anuncios: String = "",
    val chamados: String = "",
    val desobrigacoes: String = "",
    val observacoes: String = "",
    val hinos: MeetingHymns = MeetingHymns(),
    val oracoes: MeetingPrayers = MeetingPrayers(),
    val oradores: List<MeetingSpeaker> = listOf(MeetingSpeaker())
)

@Serializable
data class MeetingHymns(
    val abertura: String = "",
    val sacramento: String = "",
    val intermediario: String = "",
    val encerramento: String = ""
)

@Serializable
data class MeetingPrayers(
    val abertura: String = "",
    val encerramento: String = ""
)

@Serializable
data class MeetingSpeaker(
    val nome: String = "",
    val assunto: String = ""
)
