package com.roleta.app.ui.screen.pick

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roleta.app.ui.component.SlotMachineAnimation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickScreen(
    listId: String,
    listName: String,
    onNavigateBack: () -> Unit,
    viewModel: PickViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(listId) {
        viewModel.init(listId, listName)
    }

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listName) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
            transitionSpec = {
                if (initialState is PickUiState.Spinning && targetState is PickUiState.Result) {
                    (scaleIn(
                        initialScale = 0.85f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn(animationSpec = tween(220)))
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                } else {
                    fadeIn().togetherWith(fadeOut())
                }
            },
            contentKey = { it::class.simpleName },
            label = "pick_content"
        ) { s ->
            when (s) {
                is PickUiState.Loading -> CircularProgressIndicator()

                is PickUiState.Spinning -> SpinningContent(
                    state = s,
                    onSettled = { viewModel.onAnimationSettled() }
                )

                is PickUiState.Result -> ResultContent(
                    state = s,
                    onAccept = { viewModel.onAccept() },
                    onTryAgain = { viewModel.onTryAgain() }
                )
            }
        }
    }
}

@Composable
private fun SpinningContent(
    state: PickUiState.Spinning,
    onSettled: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        SlotMachineAnimation(
            items = state.items,
            targetIndex = state.targetIndex,
            onSettled = onSettled
        )
        Text(
            text = state.quip,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )
    }
}

@Composable
private fun ResultContent(
    state: PickUiState.Result,
    onAccept: () -> Unit,
    onTryAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = state.listName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = state.selectedItemText,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
            )
        }

        if (state.priorPickCount != null || state.skipCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (state.priorPickCount != null) {
            Text(
                text = "Picked ${state.priorPickCount} time${if (state.priorPickCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.skipCount > 0) {
            Text(
                text = "Skipped ${state.skipCount} time${if (state.skipCount == 1) "" else "s"} this round",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onTryAgain,
                modifier = Modifier.weight(1f)
            ) {
                Text("Try Again")
            }
            Button(
                onClick = onAccept,
                modifier = Modifier.weight(1f)
            ) {
                Text("Accept")
            }
        }
    }
}
