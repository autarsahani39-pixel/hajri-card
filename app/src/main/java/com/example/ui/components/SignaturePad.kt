package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SignatureRecord
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun SignaturePadSection(
    signatureRecord: SignatureRecord?,
    onSaveSignature: (String) -> Unit,
    onClearSignature: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    val currentStrokes = remember { mutableStateListOf<List<Offset>>() }
    var currentPathPoints = remember { mutableListOf<Offset>() }

    // Load initial signature if exists
    LaunchedEffect(signatureRecord) {
        if (signatureRecord == null || signatureRecord.signatureSvg.isBlank()) {
            currentStrokes.clear()
            isEditing = true
        } else {
            isEditing = false
            currentStrokes.clear()
            val parsed = parseSignaturePoints(signatureRecord.signatureSvg)
            currentStrokes.addAll(parsed)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("signature_section"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RECEIVER'S SIGNATURE / THUMB IMPRESSION",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Worker Signature or Fingerprint",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (signatureRecord != null && signatureRecord.signatureSvg.isNotBlank() && !isEditing) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PresentBgColor,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PresentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Saved",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PresentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(
                        width = 1.5.dp,
                        color = if (isEditing) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("signature_canvas")
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isEditing) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPathPoints = mutableListOf(offset)
                                            currentStrokes.add(currentPathPoints.toList())
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentPathPoints.add(change.position)
                                            if (currentStrokes.isNotEmpty()) {
                                                currentStrokes[currentStrokes.lastIndex] = currentPathPoints.toList()
                                            }
                                        },
                                        onDragEnd = {
                                            // Finish stroke
                                        }
                                    )
                                }
                            } else Modifier
                        )
                ) {
                    for (stroke in currentStrokes) {
                        if (stroke.size > 1) {
                            val path = Path()
                            path.moveTo(stroke.first().x, stroke.first().y)
                            for (i in 1 until stroke.size) {
                                path.lineTo(stroke[i].x, stroke[i].y)
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF1E3A8A), // Deep signature blue ink
                                style = Stroke(
                                    width = 4f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        } else if (stroke.size == 1) {
                            drawCircle(
                                color = Color(0xFF1E3A8A),
                                radius = 3f,
                                center = stroke.first()
                            )
                        }
                    }
                }

                if (currentStrokes.isEmpty() && isEditing) {
                    Text(
                        text = "Sign or draw thumb impression here with finger",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEditing) {
                    OutlinedButton(
                        onClick = {
                            currentStrokes.clear()
                            currentPathPoints.clear()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_signature_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (currentStrokes.isNotEmpty()) {
                                val json = serializeSignaturePoints(currentStrokes)
                                onSaveSignature(json)
                                isEditing = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_signature_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = currentStrokes.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            isEditing = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_signature_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-sign", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            currentStrokes.clear()
                            currentPathPoints.clear()
                            onClearSignature()
                            isEditing = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delete_signature_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun serializeSignaturePoints(strokes: List<List<Offset>>): String {
    val array = JSONArray()
    for (stroke in strokes) {
        val sArr = JSONArray()
        for (pt in stroke) {
            val obj = JSONObject()
            obj.put("x", pt.x.toDouble())
            obj.put("y", pt.y.toDouble())
            sArr.put(obj)
        }
        array.put(sArr)
    }
    return array.toString()
}

private fun parseSignaturePoints(json: String): List<List<Offset>> {
    val list = mutableListOf<List<Offset>>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val strokeArray = array.getJSONArray(i)
            val stroke = mutableListOf<Offset>()
            for (j in 0 until strokeArray.length()) {
                val obj = strokeArray.getJSONObject(j)
                val x = obj.getDouble("x").toFloat()
                val y = obj.getDouble("y").toFloat()
                stroke.add(Offset(x, y))
            }
            list.add(stroke)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
