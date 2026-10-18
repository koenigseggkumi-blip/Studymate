package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Assignment
import com.example.data.model.Course
import com.example.ui.components.AssignmentCard
import com.example.ui.components.AssignmentEditorDialog
import com.example.ui.components.CourseManagerDialog
import com.example.ui.components.NewAssignmentInputComponent
import com.example.ui.components.UrgencyHeaderBanner
import com.example.util.DateUtils
import com.example.util.UrgencyLevel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AssignmentViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val credentialManager = remember { CredentialManager.create(context) }

    val assignmentsState by viewModel.assignmentsState.collectAsStateWithLifecycle()
    val courses by viewModel.coursesState.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedCourseName by viewModel.selectedCourseName.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    var showEditorDialog by remember { mutableStateOf(false) }
    var showNewAssignmentSheet by remember { mutableStateOf(false) }
    var editingAssignment by remember { mutableStateOf<Assignment?>(null) }
    var showCourseManagerDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmation by remember { mutableStateOf(false) }

    // Seed starter courses if empty
    LaunchedEffect(courses) {
        if (courses.isEmpty()) {
            viewModel.seedStarterCoursesIfEmpty(courses)
        }
    }

    // Snackbars
    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.duetrack_icon),
                            contentDescription = "DueTrack",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DueTrack",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val email = currentUser?.email ?: "Synced"
                            Text(
                                text = email,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCourseManagerDialog = true },
                        modifier = Modifier.testTag("manage_courses_button")
                    ) {
                        Icon(Icons.Default.School, contentDescription = "Manage Courses")
                    }
                    IconButton(
                        onClick = { showSignOutConfirmation = true },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    showNewAssignmentSheet = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Assignment") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_assignment_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = assignmentsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Could not load assignments",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val allAssignments = state.data

                    // Filter and search
                    val filteredAssignments = allAssignments.filter { assignment ->
                        // Search query
                        val matchesSearch = searchQuery.isBlank() ||
                                assignment.title.contains(searchQuery, ignoreCase = true) ||
                                assignment.courseName.contains(searchQuery, ignoreCase = true) ||
                                (assignment.notes?.contains(searchQuery, ignoreCase = true) == true)

                        // Course filter
                        val matchesCourse = selectedCourseName == null || assignment.courseName == selectedCourseName

                        // Category filter
                        val urgency = DateUtils.getUrgencyLevel(assignment.dueTimestamp)
                        val matchesFilter = when (selectedFilter) {
                            FilterType.ALL -> true
                            FilterType.DUE_SOON -> assignment.status != "COMPLETED" && (urgency == UrgencyLevel.OVERDUE || urgency == UrgencyLevel.DUE_TODAY || urgency == UrgencyLevel.DUE_SOON)
                            FilterType.OVERDUE -> assignment.status != "COMPLETED" && urgency == UrgencyLevel.OVERDUE
                            FilterType.TODAY -> assignment.status != "COMPLETED" && urgency == UrgencyLevel.DUE_TODAY
                            FilterType.COMPLETED -> assignment.status == "COMPLETED"
                        }

                        matchesSearch && matchesCourse && matchesFilter
                    }.sortedWith(
                        compareBy<Assignment> { it.status == "COMPLETED" }
                            .thenBy {
                                when (it.priority) {
                                    "HIGH" -> 0
                                    "MEDIUM" -> 1
                                    else -> 2
                                }
                            }
                            .thenBy { it.dueTimestamp?.seconds ?: Long.MAX_VALUE }
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Overview Banner
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                UrgencyHeaderBanner(
                                    assignments = allAssignments,
                                    onSelectFilter = { viewModel.setFilter(it) }
                                )

                                // Search Bar
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = "Search")
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                            }
                                        }
                                    },
                                    placeholder = { Text("Search assignments, courses, notes...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search_assignment_input")
                                )

                                // Filter Chips Scrollable Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterType.values().forEach { filter ->
                                        val isSelected = selectedFilter == filter
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setFilter(filter) },
                                            label = { Text(filter.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }

                                // Course Chips Row (if courses exist)
                                if (courses.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Courses:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        FilterChip(
                                            selected = selectedCourseName == null,
                                            onClick = { viewModel.setSelectedCourse(null) },
                                            label = { Text("All Courses") }
                                        )

                                        courses.forEach { course ->
                                            val isSelected = selectedCourseName == course.name
                                            val color = try {
                                                Color(android.graphics.Color.parseColor(course.colorHex))
                                            } catch (e: Exception) {
                                                MaterialTheme.colorScheme.primary
                                            }

                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    viewModel.setSelectedCourse(
                                                        if (isSelected) null else course.name
                                                    )
                                                },
                                                leadingIcon = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(color, CircleShape)
                                                    )
                                                },
                                                label = { Text(course.code) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // List Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedFilter.label} (${filteredAssignments.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (filteredAssignments.isNotEmpty()) {
                                    Text(
                                        text = "Sorted by Urgency",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Empty State
                        if (filteredAssignments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            modifier = Modifier.size(72.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Assignment,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (allAssignments.isEmpty()) {
                                                "You have no assignments yet!"
                                            } else {
                                                "No assignments matching this filter"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = if (allAssignments.isEmpty()) {
                                                "Tap '+ New Assignment' to start tracking your deadlines and never forget a due date."
                                            } else {
                                                "Try switching to 'All' or clearing your search term."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            // Assignment Cards
                            items(filteredAssignments, key = { it.id }) { assignment ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    AssignmentCard(
                                        assignment = assignment,
                                        onClick = {
                                            editingAssignment = assignment
                                            showEditorDialog = true
                                        },
                                        onToggleComplete = {
                                            viewModel.toggleAssignmentCompleted(assignment)
                                        },
                                        onToggleSubtask = { subtaskId ->
                                            viewModel.toggleSubtask(assignment, subtaskId)
                                        },
                                        onDelete = {
                                            viewModel.deleteAssignment(assignment.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New Assignment Modal Bottom Sheet with Date Picker & Text Fields
    if (showNewAssignmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNewAssignmentSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            NewAssignmentInputComponent(
                courses = courses,
                onSaveAssignment = { title, courseName, colorHex, dueTimestamp, prio, notes, est, subtasks ->
                    viewModel.createAssignment(
                        title = title,
                        courseName = courseName,
                        courseColorHex = colorHex,
                        dueTimestamp = dueTimestamp,
                        priority = prio,
                        notes = notes,
                        estimatedMinutes = est,
                        subtasks = subtasks
                    )
                    showNewAssignmentSheet = false
                },
                onCancel = { showNewAssignmentSheet = false },
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }

    // Add / Edit Assignment Dialog
    if (showEditorDialog) {
        AssignmentEditorDialog(
            initialAssignment = editingAssignment,
            courses = courses,
            onDismiss = { showEditorDialog = false },
            onSave = { title, courseName, colorHex, dueTimestamp, prio, notes, est, subtasks ->
                if (editingAssignment == null) {
                    viewModel.createAssignment(
                        title = title,
                        courseName = courseName,
                        courseColorHex = colorHex,
                        dueTimestamp = dueTimestamp,
                        priority = prio,
                        notes = notes,
                        estimatedMinutes = est,
                        subtasks = subtasks
                    )
                } else {
                    val updated = editingAssignment!!.copy(
                        title = title,
                        courseName = courseName,
                        courseColorHex = colorHex,
                        dueTimestamp = dueTimestamp,
                        priority = prio,
                        notes = notes,
                        estimatedMinutes = est,
                        subtasks = subtasks.map { it.toMap() }
                    )
                    viewModel.updateAssignment(updated)
                }
                showEditorDialog = false
            },
            onRequestNewCourse = {
                showCourseManagerDialog = true
            }
        )
    }

    // Course Manager Dialog
    if (showCourseManagerDialog) {
        CourseManagerDialog(
            courses = courses,
            onDismiss = { showCourseManagerDialog = false },
            onCreateCourse = { name, code, colorHex ->
                viewModel.createCourse(name, code, colorHex)
            },
            onDeleteCourse = { courseId ->
                viewModel.deleteCourse(courseId)
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmation = false },
            title = { Text("Sign Out of DueTrack?") },
            text = { Text("Your assignments and courses remain securely saved in your Google Account. You can sign back in anytime.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirmation = false
                        signOutUser(credentialManager, onSignOutComplete = onSignOut, scope = coroutineScope)
                    }
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
