package com.merryblue.baseapplication.helpers

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import timber.log.Timber

fun Fragment.findNavControllerOrNull(): NavController? = runCatching { findNavController() }.getOrNull()

fun Fragment.safeNavigate(
    @IdRes resId: Int,
    args: Bundle? = null,
) {
    runCatching {
        val navController = findNavControllerOrNull() ?: return@runCatching
        if (navController.hasAction(resId)) {
            navController.navigate(resId, args)
        }
    }
}

fun Fragment.safeNavigate(navDirections: NavDirections) {
    val navController = findNavControllerOrNull() ?: return
    runCatching {
        navController.navigate(navDirections)
    }
}

fun Fragment.safeNavigateParentNav(
    navHostId: Int,
    navDirections: NavDirections
) {
    runCatching {
        requireActivity()
            .findNavController(navHostId)
            .navigate(navDirections)
    }
}

fun NavController.hasAction(@IdRes actionId: Int): Boolean {
    val destination = currentDestination ?: return false
    return destination.getAction(actionId) != null
}

fun Fragment.safePopBackStack(
    destinationId: Int? = null,
    inclusive: Boolean = false
) {
    findNavControllerOrNull()?.let { navController ->
        if (destinationId != null) {
            navController.popBackStack(destinationId, inclusive)
        } else {
            navController.popBackStack()
        }
    }
}

fun NavController.navigateSingleTop(@IdRes destinationId: Int) {
    val options = NavOptions.Builder()
        .setLaunchSingleTop(true)
        .setPopUpTo(
            currentDestination?.id ?: graph.startDestinationId,
            false
        )
        .build()

    navigate(destinationId, null, options)
}