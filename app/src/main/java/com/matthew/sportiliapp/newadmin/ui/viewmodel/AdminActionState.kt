package com.matthew.sportiliapp.newadmin.ui.viewmodel

sealed interface AdminActionState {
    object Idle : AdminActionState
    object InProgress : AdminActionState
    data class Error(val message: String) : AdminActionState
}
