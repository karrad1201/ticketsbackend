package com.karrad.bilets.web

import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.support.MockMvcSecurityHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MyOrganizationMembersControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var webApplicationContext: WebApplicationContext
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var authTokenRepository: AuthTokenRepository

    private val orgId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    private val ownerId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002")
    private val managerId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000003")
    private val outsiderId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000004")
    private val targetUserId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000005")
    private val venueId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000006")

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .let { MockMvcSecurityHelper.withSpringSecurity(it) }
            .build()

        organizationRepository.save(Organization(code = "test-org", name = "Test Org", id = orgId))
        userRepository.save(User(email = "owner@test.com", fullName = "Owner", id = ownerId))
        userRepository.save(User(email = "manager@test.com", fullName = "Manager", id = managerId))
        userRepository.save(User(email = "outsider@test.com", fullName = "Outsider", id = outsiderId))
        userRepository.save(User(email = "target@test.com", fullName = "Target", id = targetUserId))

        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = ownerId, role = OrganizationMemberRole.OWNER)
        )
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = managerId, role = OrganizationMemberRole.MANAGER)
        )
    }

    @Test
    fun `OWNER can list members`() {
        mockMvc.perform(
            get("/api/v1/my/organization/members")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(ownerId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `MANAGER can list members`() {
        mockMvc.perform(
            get("/api/v1/my/organization/members")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(managerId)}")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `non-member gets 403 on list`() {
        mockMvc.perform(
            get("/api/v1/my/organization/members")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(outsiderId)}")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `OWNER can add MANAGER`() {
        mockMvc.perform(
            post("/api/v1/my/organization/members")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(ownerId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"$targetUserId","role":"MANAGER","venueId":"$venueId"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.role").value("MANAGER"))
    }

    @Test
    fun `MANAGER cannot add MANAGER — gets 403`() {
        mockMvc.perform(
            post("/api/v1/my/organization/members")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(managerId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"$targetUserId","role":"MANAGER"}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `OWNER can update member role`() {
        val member = organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = targetUserId, role = OrganizationMemberRole.MANAGER)
        )

        mockMvc.perform(
            put("/api/v1/my/organization/members/${member.id}")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(ownerId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"role":"MANAGER"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("MANAGER"))
    }

    @Test
    fun `MANAGER cannot update roles — gets 403`() {
        val member = organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = targetUserId, role = OrganizationMemberRole.STAFF)
        )

        mockMvc.perform(
            put("/api/v1/my/organization/members/${member.id}")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(managerId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"role":"MANAGER"}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `OWNER can delete any member`() {
        val member = organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = targetUserId, role = OrganizationMemberRole.MANAGER)
        )

        mockMvc.perform(
            delete("/api/v1/my/organization/members/${member.id}")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(ownerId)}")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `MANAGER can delete STAFF but not MANAGER`() {
        val staffMember = organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = targetUserId, role = OrganizationMemberRole.STAFF)
        )

        // MANAGER удаляет STAFF — ок
        mockMvc.perform(
            delete("/api/v1/my/organization/members/${staffMember.id}")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(managerId)}")
        )
            .andExpect(status().isNoContent)

        // MANAGER пытается удалить другого MANAGER — 403
        val managerMember = organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = targetUserId, role = OrganizationMemberRole.MANAGER)
        )
        mockMvc.perform(
            delete("/api/v1/my/organization/members/${managerMember.id}")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(managerId)}")
        )
            .andExpect(status().isForbidden)
    }
}
