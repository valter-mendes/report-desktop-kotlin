package ao.cmc.fincrestsdvm.data.models

import kotlinx.serialization.Serializable

@Serializable
data class SiraUser(
    val email: String,
    val name: String
)

@Serializable
data class SiraLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class SiraLoginResponse(
    val token: String,
    val user: SiraUser
)
