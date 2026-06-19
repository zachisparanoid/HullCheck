package com.hullcheck.ui.navigation

/** Central definition of navigation routes and their argument keys. */
object Routes {
    const val ARG_PROJECT_ID = "projectId"
    const val ARG_CATEGORY_ID = "categoryId"
    const val ARG_ITEM_ID = "itemId"

    const val PROJECTS = "projects"
    const val SETTINGS = "settings"
    const val TEMPLATES = "templates"

    const val PROJECT_EDITOR = "projectEditor?projectId={projectId}"
    fun projectEditor(projectId: String? = null): String =
        if (projectId == null) "projectEditor" else "projectEditor?projectId=$projectId"

    const val PROJECT_DETAIL = "projectDetail/{projectId}"
    fun projectDetail(projectId: String): String = "projectDetail/$projectId"

    const val CHECKLIST = "checklist/{projectId}/{categoryId}"
    fun checklist(projectId: String, categoryId: String): String = "checklist/$projectId/$categoryId"

    const val ITEM_DETAIL = "itemDetail/{itemId}"
    fun itemDetail(itemId: String): String = "itemDetail/$itemId"

    const val DEFECTS = "defects/{projectId}"
    fun defects(projectId: String): String = "defects/$projectId"

    const val REPORT = "report/{projectId}"
    fun report(projectId: String): String = "report/$projectId"
}
