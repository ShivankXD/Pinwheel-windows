package com.pinwheel.core.model

/** Recipes only: no nested projects, source copies or rendered bitmaps. */
data class PhotoEdit(val name: String, val adjustments: Adjustments) {
    companion object { fun of(project: StudioProject) = PhotoEdit(project.name, project.adjustments) }
}

data class PhotoHistory(val past: List<PhotoEdit> = emptyList(), val future: List<PhotoEdit> = emptyList()) {
    fun record(project: StudioProject) = PhotoHistory((past + PhotoEdit.of(project)).takeLast(LIMIT))
    fun undo(project: StudioProject): StudioProject? = past.lastOrNull()?.let {
        project.copy(name = it.name, adjustments = it.adjustments,
            photoHistory = PhotoHistory(past.dropLast(1), (future + PhotoEdit.of(project)).takeLast(LIMIT)))
    }
    fun redo(project: StudioProject): StudioProject? = future.lastOrNull()?.let {
        project.copy(name = it.name, adjustments = it.adjustments,
            photoHistory = PhotoHistory((past + PhotoEdit.of(project)).takeLast(LIMIT), future.dropLast(1)))
    }
    companion object { const val LIMIT = 40 }
}
