package store

class NoSuchKeyException(val key: String) : RuntimeException("no such key: $key")
