package com.karrad.bilets.web

import com.karrad.bilets.application.service.UserService
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.web.dto.UserResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/auth/me")
class UserProfileController(
    private val userService: UserService,
    private val currentUserProvider: CurrentUserProvider
) {
    @PatchMapping
    fun updateProfile(@RequestBody body: UpdateProfileRequest): UserResponse {
        val userId = currentUserProvider.requireUserId()
        return userService.updateProfile(userId, body.fullName, body.interests).toResponse()
    }

    @PostMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAvatar(@RequestParam("file") file: MultipartFile): UserResponse {
        val userId = currentUserProvider.requireUserId()
        val ext = (file.originalFilename?.substringAfterLast('.', "jpg") ?: "jpg")
            .lowercase()
            .filter { it.isLetterOrDigit() }
            .take(5)
            .ifBlank { "jpg" }
        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dir = Paths.get("uploads/avatars/$date")
        Files.createDirectories(dir)
        val filename = "$userId.$ext"
        val targetPath = dir.resolve(filename)
        file.transferTo(targetPath)
        val avatarUrl = "/uploads/avatars/$date/$filename"
        return userService.updateAvatar(userId, avatarUrl).toResponse()
    }

    private fun User.toResponse() = UserResponse(
        id = id.toString(),
        fullName = fullName,
        phone = phone,
        email = email,
        role = role.name,
        avatarUrl = avatarUrl,
        interests = interests
    )
}

data class UpdateProfileRequest(
    val fullName: String? = null,
    val interests: List<String>? = null
)
