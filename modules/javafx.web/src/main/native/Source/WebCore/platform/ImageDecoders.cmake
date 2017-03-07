list(APPEND WebCore_INCLUDE_DIRECTORIES
    "${WEBCORE_DIR}/platform/image-decoders"
    "${WEBCORE_DIR}/platform/image-decoders/bmp"
    "${WEBCORE_DIR}/platform/image-decoders/gif"
    "${WEBCORE_DIR}/platform/image-decoders/ico"
    "${WEBCORE_DIR}/platform/image-decoders/jpeg"
    "${WEBCORE_DIR}/platform/image-decoders/png"
    "${WEBCORE_DIR}/platform/image-decoders/webp"
)

list(APPEND WebCore_SOURCES
    platform/image-decoders/ImageDecoder.cpp

    platform/image-decoders/bmp/BMPImageDecoder.cpp
    platform/image-decoders/bmp/BMPImageReader.cpp

    platform/image-decoders/gif/GIFImageDecoder.cpp
    platform/image-decoders/gif/GIFImageReader.cpp

    platform/image-decoders/ico/ICOImageDecoder.cpp

    platform/image-decoders/jpeg/JPEGImageDecoder.cpp

    platform/image-decoders/png/PNGImageDecoder.cpp

    platform/image-decoders/webp/WEBPImageDecoder.cpp
)

if (JPEG_FOUND)
    list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
        ${JPEG_INCLUDE_DIR}
    )
    list(APPEND WebCore_LIBRARIES
        ${JPEG_LIBRARIES}
    )
endif ()

if (PNG_FOUND)
    list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
        ${PNG_INCLUDE_DIRS}
    )
    list(APPEND WebCore_LIBRARIES
        ${PNG_LIBRARIES}
    )
endif ()

if (WEBP_FOUND)
    list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
        ${WEBP_INCLUDE_DIRS}
    )
    list(APPEND WebCore_LIBRARIES
        ${WEBP_LIBRARIES}
    )
endif ()
