package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Assignment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class AssignmentRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAssignment_validPayload_createsDocumentAndReturnsId() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = AssignmentRepository(firestore)

    val assignment = Assignment(
      title = "Chemistry Lab 3",
      courseName = "Chemistry",
      courseColorHex = "#10B981",
      dueTimestamp = Timestamp.now(),
      priority = "HIGH",
      status = "NOT_STARTED"
    )

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createAssignment(uid, assignment)
    }
    assertTrue(createResult.isSuccess)
    val assignmentId = createResult.getOrThrow()
    assertTrue(assignmentId.isNotEmpty())
  }

  @Test
  fun observeAssignments_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = AssignmentRepository(firestore)

    val testTitle = "Biology Chapter " + UUID.randomUUID().toString().take(6)
    val assignment = Assignment(
      title = testTitle,
      courseName = "Biology",
      courseColorHex = "#3B82F6",
      dueTimestamp = Timestamp.now(),
      priority = "MEDIUM",
      status = "NOT_STARTED"
    )

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createAssignment(uid, assignment)
    }
    assertTrue(createResult.isSuccess)
    val assignmentId = createResult.getOrThrow()

    val emittedAssignments = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeAssignments(uid).first { list -> list.any { it.id == assignmentId } }
    }
    assertTrue(emittedAssignments.any { it.id == assignmentId })
  }

  @Test
  fun getAssignmentById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = AssignmentRepository(firestore)

    val assignment = Assignment(
      title = "Alice Secret Project",
      courseName = "Honors Thesis",
      courseColorHex = "#8B5CF6",
      dueTimestamp = Timestamp.now(),
      priority = "HIGH",
      status = "NOT_STARTED"
    )

    val assignmentId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createAssignment(aliceUid, assignment).getOrThrow()
    }

    val bobUid = signInTestUser(BOB_EMAIL)
    val bobRepo = AssignmentRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getAssignment(aliceUid, assignmentId)
    }
    assertTrue(result.isFailure)
    val exception = result.exceptionOrNull() as? FirebaseFirestoreException
    assertNotNull(exception)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
  }

  @Test
  fun observeAssignments_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = AssignmentRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeAssignments("alice_unauthenticated_test").first()
      }
      fail("Expected FirebaseFirestoreException but succeeded")
    } catch (e: Exception) {
      val firestoreException = (e as? FirebaseFirestoreException)
        ?: (e.cause as? FirebaseFirestoreException)
        ?: (e.cause?.cause as? FirebaseFirestoreException)
      assertNotNull("Expected FirebaseFirestoreException in cause chain", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@example.com"
    const val BOB_EMAIL = "bob@example.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
