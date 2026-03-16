package data

import trainer.Word

class DictionaryRepository(private val filePath: String) {

    private val delegate = FileUserDictionary(filePath)

    fun load(): MutableList<Word> {
        return delegate.getAllWords().toMutableList()
    }

    fun save(dictionary: List<Word>) {
        delegate.saveAll(dictionary)
    }

    fun resetProgress() {
        delegate.resetUserProgress()
    }
}