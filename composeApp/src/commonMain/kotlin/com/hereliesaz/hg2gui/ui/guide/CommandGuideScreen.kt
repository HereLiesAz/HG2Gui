package com.hereliesaz.hg2gui.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.hg2gui.ui.menu.Azphalt

@Composable
fun CommandGuideScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(Azphalt.White).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Command Guide",
                style = MaterialTheme.typography.headlineMedium,
                color = Azphalt.Ink
            )
            Text(
                text = "X",
                style = MaterialTheme.typography.headlineMedium,
                color = Azphalt.Ink,
                modifier = Modifier.clickable { onBack() }
            )
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Text(
                    text = "Handbook Reference",
                    style = MaterialTheme.typography.titleMedium,
                    color = Azphalt.Ink.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                PillReturnFormatter(nodes = CommandHandbookData.handbookNodes)
            }
        }
    }
}
