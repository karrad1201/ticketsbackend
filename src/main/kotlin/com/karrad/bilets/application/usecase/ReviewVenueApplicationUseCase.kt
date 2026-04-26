package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueApplication
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueApplicationRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

@Component
class ReviewVenueApplicationUseCase(
    private val userRepository: UserRepository,
    private val venueApplicationRepository: VenueApplicationRepository,
    private val venueRepository: VenueRepository,
    private val clock: Clock
) {
    fun approve(applicationId: UUID, adminUserId: UUID): VenueApplication {
        val admin = requireNotNull(userRepository.findById(adminUserId)) { "Admin not found: $adminUserId" }
        require(admin.role == UserRole.ADMIN) { "Reviewer must be admin: $adminUserId" }

        val application = requireNotNull(venueApplicationRepository.findById(applicationId)) {
            "VenueApplication not found: $applicationId"
        }

        val venue = venueRepository.save(
            Venue(
                label = application.name,
                city = City(
                    label = application.cityLabel,
                    subject = Subject(label = application.subjectLabel)
                ),
                organizationId = application.organizationId,
                address = application.address
            )
        )

        return venueApplicationRepository.save(
            application.approve(adminUserId = admin.id, approvedVenueId = venue.id, at = clock.instant())
        )
    }

    fun reject(applicationId: UUID, adminUserId: UUID): VenueApplication {
        val admin = requireNotNull(userRepository.findById(adminUserId)) { "Admin not found: $adminUserId" }
        require(admin.role == UserRole.ADMIN) { "Reviewer must be admin: $adminUserId" }

        val application = requireNotNull(venueApplicationRepository.findById(applicationId)) {
            "VenueApplication not found: $applicationId"
        }

        return venueApplicationRepository.save(application.reject(adminUserId = admin.id, at = clock.instant()))
    }
}
