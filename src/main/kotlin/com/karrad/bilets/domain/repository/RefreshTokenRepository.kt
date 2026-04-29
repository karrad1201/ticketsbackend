package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.RefreshToken
import java.util.UUID

interface RefreshTokenRepository {
    fun save(refreshToken: RefreshToken): RefreshToken
    fun findByToken(token: String): RefreshToken?
    fun deleteByToken(token: String)
    fun deleteByUserId(userId: UUID)
}
