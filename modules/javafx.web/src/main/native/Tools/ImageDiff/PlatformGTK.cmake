set(IMAGE_DIFF_SOURCES
    ${IMAGE_DIFF_DIR}/gtk/ImageDiff.cpp
)

list(APPEND IMAGE_DIFF_SYSTEM_INCLUDE_DIRECTORIES
    ${GTK_INCLUDE_DIRS}
)

list(APPEND IMAGE_DIFF_LIBRARIES
    ${GTK_LIBRARIES}
)
