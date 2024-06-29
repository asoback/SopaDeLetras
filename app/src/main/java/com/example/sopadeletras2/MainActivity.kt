package com.example.sopadeletras2

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var lettersGrid: GridLayout
    private lateinit var findWordsGrid: GridLayout
    private var selectedCells = mutableListOf<TextView>()
    private var isDragging = false
    private var direction: Direction? = null
    private lateinit var selectedWords : List<Word>

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val allWords = loadWordsFromJson()
        // Shuffle the list and take the first 12 words
        selectedWords = allWords.shuffled().take(12)

        findWordsGrid = findViewById(R.id.findWordsGrid)
        lettersGrid = findViewById(R.id.lettersGrid)

        lettersGrid.rowCount = 10
        lettersGrid.columnCount = 10

        // Wait for the layout to be fully inflated
        val viewTreeObserver = lettersGrid.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Remove the listener to prevent multiple calls
                lettersGrid.viewTreeObserver.removeOnGlobalLayoutListener(this)
                setupLettersGrid(selectedWords)
                setupWordsGrid(selectedWords)
            }
        })
    }

    private fun setupWordsGrid(selectedWords : List<Word>) {

        for (word in selectedWords) {
            val textView = TextView(this).apply {
                text = word.text
                textSize = 18f
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    onWordClick(this, word)
                }
                updateTextViewAppearance(this, word)
            }

            val param = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
            }

            textView.layoutParams = param
            findWordsGrid.addView(textView)
        }
    }

    private fun setupLettersGrid(selectedWords : List<Word>) {
        val chars = ('a'..'z') + 'ñ' + 'á'
        val letters = List(100) { chars.random() }

        for (letter in letters) {
            val textView = TextView(this).apply {
                text = letter.toString()
                textSize = 18f
                setPadding(16, 16, 16, 16)
                setBackgroundColor(Color.LTGRAY)
                setTextColor(Color.BLACK)
                setOnTouchListener { view, event ->
                    handleTouch(view as TextView, event)
                }
            }

            val param = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
            }

            textView.layoutParams = param
            lettersGrid.addView(textView)
        }

        // TODO add the words
        val cols = lettersGrid.columnCount
        println("Cols ${cols}")

        if (selectedWords[0].text.length <= cols) {
            for (i in 0..<selectedWords[0].text.length) {
                if (lettersGrid.getChildAt(i) is TextView) {
                    val textView = lettersGrid.getChildAt(i) as TextView
                    textView.setText(selectedWords[0].text.get(i).toString())
                }
            }
        }
    }

    private fun loadWordsFromJson(): List<Word> {
        val inputStream = resources.openRawResource(R.raw.words)
        val reader = InputStreamReader(inputStream)
        val wordType = object : TypeToken<List<Word>>() {}.type
        return Gson().fromJson(reader, wordType)
    }

    private fun handleTouch(view: TextView, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                direction = null
                selectedCells.clear()
                selectCell(view)
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val cell = getCellAt(event.rawX.toInt(), event.rawY.toInt())
                    cell?.let { selectCell(it) }
                }
            }
            MotionEvent.ACTION_UP -> {
                isDragging = false
                highlightSelectedCells()
                var str : String = ""
                for (cell in selectedCells) {
                    str += cell.text
                }
                var foundStr = false
                for (word in selectedWords) {
                    if (word.text == str) {
                        println("Found ${str}")
                        word.isCrossedOut = true
                        foundStr = true
                        for (i in 0..<findWordsGrid.childCount) {
                            val tv = findWordsGrid.getChildAt(i) as TextView
                            if (tv.text == str) {
                                updateTextViewAppearance(tv, word)
                                break
                            }
                        }
                    }
                }
                if (!foundStr) {
                    unhighlightSelectedCells()
                }
            }
        }
        return true
    }

    private fun getCellAt(x: Int, y: Int): TextView? {
        for (i in 0 until lettersGrid.childCount) {
            val child = lettersGrid.getChildAt(i)
            val location = IntArray(2)
            child.getLocationOnScreen(location)
            if (x >= location[0] && x <= location[0] + child.width &&
                y >= location[1] && y <= location[1] + child.height) {

                // Determine the direction after the second cell is selected
                if (selectedCells.size == 1 && direction == null) {
                    val firstCellLocation = IntArray(2)
                    selectedCells[0].getLocationOnScreen(firstCellLocation)

                    direction = if (firstCellLocation[0] == location[0] && firstCellLocation[1] == location[1]) {
                        null     // Ignore if it is still the same cell
                    }
                    else if (firstCellLocation[0] == location[0]) {
                        Direction.VERTICAL
                    } else if (firstCellLocation[1] == location[1]) {
                        Direction.HORIZONTAL
                    } else {
                        null
                    }
                }

                // Ensure the new cell is in the same row or column based on direction
                val firstCellLocation = IntArray(2)
                selectedCells[0].getLocationOnScreen(firstCellLocation)
                // TODO: Make sure they are contiguous cells
                return when (direction) {
                    Direction.VERTICAL -> if (firstCellLocation[0] == location[0]) child as TextView else null
                    Direction.HORIZONTAL -> if (firstCellLocation[1] == location[1]) child as TextView else null
                    else -> child as TextView
                }
            }
        }
        return null
    }

    private fun selectCell(view: TextView) {
        view.setBackgroundColor(Color.YELLOW)
        if (!selectedCells.contains(view)) {
            selectedCells.add(view)
        }
    }

    private fun highlightSelectedCells() {
        for (cell in selectedCells) {
            cell.setBackgroundColor(Color.GREEN)
        }
    }

    private fun unhighlightSelectedCells() {
        for (cell in selectedCells) {
            cell.setBackgroundColor(Color.LTGRAY)
        }
    }

    private fun GridLayout.findViewAtPosition(x: Int, y: Int): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val location = IntArray(2)
            child.getLocationOnScreen(location)
            if (x >= location[0] && x <= location[0] + child.width &&
                y >= location[1] && y <= location[1] + child.height) {
                return child
            }
        }
        return null
    }


    private fun onWordClick(textView: TextView, word: Word) {
        if (textView.text == word.text) {
            textView.text = word.altText
        } else {
            textView.text = word.text
        }
    }

    private fun updateTextViewAppearance(textView: TextView, word: Word) {
        textView.paintFlags = if (word.isCrossedOut) {
            textView.setBackgroundColor(ContextCompat.getColor(this, R.color.crossedOutBackgroundColor))
            textView.setTextColor(ContextCompat.getColor(this, R.color.crossedOutTextColor))
            textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            textView.setBackgroundColor(ContextCompat.getColor(this, R.color.defaultBackgroundColor))
            textView.setTextColor(ContextCompat.getColor(this, R.color.defaultTextColor))
            textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    private enum class Direction {
        VERTICAL, HORIZONTAL
    }
}
