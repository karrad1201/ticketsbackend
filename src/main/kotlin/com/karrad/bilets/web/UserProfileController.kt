package com.karrad.bilets.web

import com.karrad.bilets.application.service.UserService
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.web.dto.UserResponse
import org.springframework.beans.factory.annotation.Value
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

private val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
private const val MAX_AVATAR_BYTES = 5 * 1024 * 1024L // 5 MB

@RestController
@RequestMapping("/auth/me")
class UserProfileController(
    private val userService: UserService,
    private val currentUserProvider: CurrentUserProvider,
    @Value("\${app.uploads.avatars-dir:uploads/avatars}") private val avatarsDir: String
) {
    @PatchMapping
    fun updateProfile(@RequestBody body: UpdateProfileRequest): UserResponse {
        val userId = currentUserProvider.requireUserId()
        return userService.updateProfile(userId, body.fullName, body.interests).toResponse()
    }

    @PostMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAvatar(@RequestParam("file") file: MultipartFile): UserResponse {
        val userId = currentUserProvider.requireUserId()

        if (file.size > MAX_AVATAR_BYTES) {
            throw IllegalArgumentException("Avatar file must not exceed 5 MB")
        }

        val rawExt = file.originalFilename?.substringAfterLast('.', "")?.lowercase() ?: ""
        val ext = if (rawExt in ALLOWED_IMAGE_EXTENSIONS) rawExt else "jpg"

        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val baseDir = Paths.get(avatarsDir).normalize()
        val dir = baseDir.resolve(date)
        Files.createDirectories(dir)
        val filename = "$userId.$ext"
        val targetPath = dir.resolve(filename)
        // Ensure the resolved path is still inside avatarsDir (path traversal guard)
        check(targetPath.startsWith(baseDir)) { "Invalid upload path" }
        file.transferTo(targetPath)
        val avatarUrl = "/$avatarsDir/$date/$filename"
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
