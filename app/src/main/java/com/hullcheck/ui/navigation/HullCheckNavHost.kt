package com.hullcheck.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sailing
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hullcheck.R
import com.hullcheck.ui.components.EmptyState
import com.hullcheck.ui.checklist.ChecklistScreen
import com.hullcheck.ui.defects.DefectsScreen
import com.hullcheck.ui.itemdetail.ItemDetailScreen
import com.hullcheck.ui.projectdetail.ProjectDetailScreen
import com.hullcheck.ui.projecteditor.ProjectEditorScreen
import com.hullcheck.ui.projects.ProjectListScreen
import com.hullcheck.ui.report.ReportScreen
import com.hullcheck.ui.settings.SettingsScreen
import com.hullcheck.ui.templates.TemplatesScreen

@Composable
fun HullCheckNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.PROJECTS,
        enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 8 } },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 8 } },
    ) {
        composable(Routes.PROJECTS) {
            BoxWithConstraints {
                // 700dp covers foldable inner displays (≈707dp) and tablets; phones stay single-pane.
                if (maxWidth >= 700.dp) {
                    ExpandedHome(navController)
                } else {
                    ProjectListScreen(
                        onOpenProject = { navController.navigate(Routes.projectDetail(it)) },
                        onNewProject = { navController.navigate(Routes.projectEditor()) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
            }
        }

        composable(
            route = Routes.PROJECT_EDITOR,
            arguments = listOf(
                navArgument(Routes.ARG_PROJECT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            ProjectEditorScreen(
                onSaved = { projectId, wasEditing ->
                    if (wasEditing) {
                        navController.popBackStack()
                    } else {
                        navController.popBackStack()
                        navController.navigate(Routes.projectDetail(projectId))
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PROJECT_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_PROJECT_ID) { type = NavType.StringType }),
        ) {
            ProjectDetailScreen(
                onBack = { navController.popBackStack() },
                onEditProject = { navController.navigate(Routes.projectEditor(it)) },
                onOpenChecklist = { projectId, categoryId -> navController.navigate(Routes.checklist(projectId, categoryId)) },
                onViewDefects = { navController.navigate(Routes.defects(it)) },
                onGenerateReport = { navController.navigate(Routes.report(it)) },
            )
        }

        composable(
            route = Routes.CHECKLIST,
            arguments = listOf(
                navArgument(Routes.ARG_PROJECT_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_CATEGORY_ID) { type = NavType.StringType },
            ),
        ) {
            ChecklistScreen(
                onBack = { navController.popBackStack() },
                onOpenItem = { navController.navigate(Routes.itemDetail(it)) },
            )
        }

        composable(
            route = Routes.ITEM_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_ITEM_ID) { type = NavType.StringType }),
        ) {
            ItemDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.DEFECTS,
            arguments = listOf(navArgument(Routes.ARG_PROJECT_ID) { type = NavType.StringType }),
        ) {
            DefectsScreen(
                onBack = { navController.popBackStack() },
                onOpenItem = { navController.navigate(Routes.itemDetail(it)) },
            )
        }

        composable(
            route = Routes.REPORT,
            arguments = listOf(navArgument(Routes.ARG_PROJECT_ID) { type = NavType.StringType }),
        ) {
            ReportScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenTemplates = { navController.navigate(Routes.TEMPLATES) },
            )
        }

        composable(Routes.TEMPLATES) {
            TemplatesScreen(onBack = { navController.popBackStack() })
        }
    }
}

private const val RIGHT_PANE_EMPTY = "home_empty"

/**
 * Expanded-width (tablet / unfolded foldable) home: project list on the left, a nested
 * detail pane on the right. The nested NavHost gives the detail ViewModel a real back-stack
 * entry (so it receives projectId), while drill-downs (checklist, report, edit) open full-screen
 * via the outer navController.
 */
@Composable
private fun ExpandedHome(outerNav: NavHostController) {
    val rightNav = rememberNavController()
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(360.dp).fillMaxHeight()) {
            ProjectListScreen(
                onOpenProject = {
                    rightNav.navigate(Routes.projectDetail(it)) {
                        popUpTo(RIGHT_PANE_EMPTY)
                        launchSingleTop = true
                    }
                },
                onNewProject = { outerNav.navigate(Routes.projectEditor()) },
                onOpenSettings = { outerNav.navigate(Routes.SETTINGS) },
            )
        }
        VerticalDivider()
        Box(Modifier.weight(1f).fillMaxHeight()) {
            NavHost(navController = rightNav, startDestination = RIGHT_PANE_EMPTY) {
                composable(RIGHT_PANE_EMPTY) {
                    EmptyState(
                        icon = Icons.Outlined.Sailing,
                        title = stringResource(R.string.select_project),
                        subtitle = stringResource(R.string.select_project_subtitle),
                    )
                }
                composable(
                    route = Routes.PROJECT_DETAIL,
                    arguments = listOf(navArgument(Routes.ARG_PROJECT_ID) { type = NavType.StringType }),
                ) {
                    ProjectDetailScreen(
                        onBack = { rightNav.navigate(RIGHT_PANE_EMPTY) { popUpTo(RIGHT_PANE_EMPTY) { inclusive = true } } },
                        onEditProject = { outerNav.navigate(Routes.projectEditor(it)) },
                        onOpenChecklist = { p, c -> outerNav.navigate(Routes.checklist(p, c)) },
                        onViewDefects = { outerNav.navigate(Routes.defects(it)) },
                        onGenerateReport = { outerNav.navigate(Routes.report(it)) },
                    )
                }
            }
        }
    }
}
