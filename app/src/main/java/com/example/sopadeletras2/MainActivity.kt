package com.example.sopadeletras2

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewTreeObserver
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var lettersGrid: GridLayout
    private lateinit var findWordsGrid: GridLayout
    private lateinit var toolbar: Toolbar
    private var selectedCells = mutableListOf<TextView>()
    private var isDragging = false
    private var direction: Direction? = null
    private lateinit var selectedWords : MutableList<Word>
    private val gridSize = 10 // 10x10 grid
    private val findWordCount = 12
    private lateinit var gridCells: Array<Array<String?>>
    private val debugMode = false
    private var colorIndex = 0

    private val colors by lazy {
        listOf(
            ContextCompat.getColor(this, R.color.color1),
            ContextCompat.getColor(this, R.color.color2),
            ContextCompat.getColor(this, R.color.color3),
            ContextCompat.getColor(this, R.color.color4),
            ContextCompat.getColor(this, R.color.color5),
            ContextCompat.getColor(this, R.color.color6)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val allWords = loadWordsFromJson()

        findWordsGrid = findViewById(R.id.findWordsGrid)
        lettersGrid = findViewById(R.id.lettersGrid)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        lettersGrid.rowCount = gridSize
        lettersGrid.columnCount = gridSize

        // Shuffle the list and take the first x words
        selectedWords = allWords.shuffled().take(findWordCount).toMutableList()

        // Wait for the layout to be fully inflated
        val viewTreeObserver = lettersGrid.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Remove the listener to prevent multiple calls
                lettersGrid.viewTreeObserver.removeOnGlobalLayoutListener(this)
                startGame()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reset -> {
                startGame()
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "2024, https://github.com/asoback/SopadeLetras", Toast.LENGTH_LONG).show()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startGame() {
        val allWords = loadWordsFromJson()
        if (selectedWords.size > 0) selectedWords.clear()
        selectedWords = allWords.shuffled().take(findWordCount).toMutableList()
        attemptPlaceWords()
        setupWordsGrid()
        setupLettersGrid()
        selectedCells.clear()
        isDragging = false
        direction = null
    }

    private fun setupWordsGrid() {
        findWordsGrid.removeAllViews()
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
        if (selectedWords.size < findWordCount) {
            for (i in (0 until findWordCount - selectedWords.size)) {
                val textView = TextView(this).apply {
                    text = " "
                    textSize = 18f
                    setPadding(16, 16, 16, 16)
                }
                val param = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                }

                val word = Word("", "", false)
                updateTextViewAppearance(textView, word)
                textView.layoutParams = param
                findWordsGrid.addView(textView)
            }
        }
    }

    private fun attemptPlaceWords() {
        gridCells = Array(gridSize) { arrayOfNulls<String>(gridSize) }
        val iterator = selectedWords.iterator()
        while (iterator.hasNext()) {
            val word = iterator.next().text
            if (!placeWordInGrid(word)) {
                iterator.remove()
                println("Failed to place ${word}")
            } else {
                println("Placed ${word}")
            }
        }
    }

    private fun checkVictory() : Boolean {
        for (word in selectedWords) {
            if (!word.isCrossedOut) {
                return false
            }
        }
        return true
    }

    private fun navigateToVictoryScreen() {
        val intent = Intent(this, VictoryActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupLettersGrid() {
        lettersGrid.removeAllViews()
        val chars = ('a'..'z') + 'ñ' + 'á' + 'í' + 'é' + 'ó' + 'ú'
        val letters = List(gridSize*gridSize) { chars.random() }

        // Fill the grid with random letters
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val textView = TextView(this).apply {
                    text = if (gridCells[i][j] != null) {
                             gridCells[i][j]
                            } else { letters[i*gridSize+j].toString() }
                    textSize = 18f
                    setPadding(16, 16, 16, 16)
                    setBackgroundColor( Color.LTGRAY)
                    setTextColor( if (debugMode && gridCells[i][j] != null) {
                        Color.BLUE
                    } else { Color.BLACK })
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
        }
    }

    private fun placeWordInGrid(word: String): Boolean {
        val random = Random.Default
        val isVertical = random.nextBoolean()
        val wordLength = word.length

        // Try to place the word in the grid without overlapping
        repeat(100) { // Limit the number of attempts
            val startRow = random.nextInt(gridSize - if (isVertical) wordLength else 1)
            val startCol = random.nextInt(gridSize - if (isVertical) 1 else wordLength)

            if (canPlaceWord(startRow, startCol, wordLength, isVertical)) {
                for (i in 0 until wordLength) {
                    val row = startRow + if (isVertical) i else 0
                    val col = startCol + if (isVertical) 0 else i

                    gridCells[row][col] = word[i].toString()
                }
                return true
            }
        }

        return false
    }

    private fun canPlaceWord(startRow: Int, startCol: Int, wordLength: Int, isVertical: Boolean): Boolean {
        for (i in 0 until wordLength) {
            val row = startRow + if (isVertical) i else 0
            val col = startCol + if (isVertical) 0 else i
            if (gridCells[row][col]?.firstOrNull()?.isLetter() == true) {
                return false
            }
        }
        return true
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
                var str = ""
                for (cell in selectedCells) {
                    str += cell.text
                }
                var foundStr = false
                for (word in selectedWords) {
                    if (word.text == str) {
                        println("Found ${str}")
                        word.isCrossedOut = true
                        colorIndex++
                        if (colorIndex >= colors.size) {
                            colorIndex = 0
                        }
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

                // check victory state
                if (checkVictory()) {
                    navigateToVictoryScreen()
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

                val firstCellLocation = IntArray(2)
                selectedCells[0].getLocationOnScreen(firstCellLocation)
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
        val bg = view.background
        if (bg is ColorDrawable) {
            if (bg.color !in colors) {
                view.setBackgroundColor(Color.YELLOW)
                if (!selectedCells.contains(view)) {
                    selectedCells.add(view)
                }
            }
        }
    }

    private fun highlightSelectedCells() {
        for (cell in selectedCells) {
            cell.setBackgroundColor(colors[colorIndex])
        }
    }

    private fun unhighlightSelectedCells() {
        for (cell in selectedCells) {
            cell.setBackgroundColor(Color.LTGRAY)
        }
    }

    private fun isDarkMode(context: Context): Boolean {
        val darkModeFlag = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return darkModeFlag == Configuration.UI_MODE_NIGHT_YES
    }

    private fun onWordClick(textView: TextView, word: Word) {
        if (textView.text == word.text) {
            textView.text = word.altText
            textView.setTextColor(Color.RED)
        } else {
            textView.text = word.text
            if (isDarkMode(context = this)) {
                textView.setTextColor(getResources().getColor(R.color.darkModeTextColorVariant))
            } else {
                textView.setTextColor(getResources().getColor(R.color.defaultTextColorVariant))
            }
        }
    }

    private fun updateTextViewAppearance(textView: TextView, word: Word) {
        textView.paintFlags = if (word.isCrossedOut) {
            textView.setBackgroundColor(ContextCompat.getColor(this, R.color.crossedOutBackgroundColor))
            textView.setTextColor(ContextCompat.getColor(this, R.color.crossedOutTextColor))
            textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            if (isDarkMode(context = this)) {
                textView.setBackgroundColor(ContextCompat.getColor(this, R.color.darkModeSecondary))
                textView.setTextColor(ContextCompat.getColor(this, R.color.darkModeTextColorVariant))
            } else {
                textView.setBackgroundColor(ContextCompat.getColor(this, R.color.defaultBackgroundColorVariant))
                textView.setTextColor(ContextCompat.getColor(this, R.color.defaultTextColorVariant))
            }
            textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    private enum class Direction {
        VERTICAL, HORIZONTAL
    }
}
