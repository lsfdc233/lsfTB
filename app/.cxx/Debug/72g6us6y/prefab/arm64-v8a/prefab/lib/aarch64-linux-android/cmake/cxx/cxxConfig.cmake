if(NOT TARGET cxx::cxx)
add_library(cxx::cxx STATIC IMPORTED)
set_target_properties(cxx::cxx PROPERTIES
    IMPORTED_LOCATION "D:/Java/.gradle/caches/9.4.1/transforms/2f236ca728f23a1fe2e62708852f7d1d/transformed/libcxx-29.0.14206865/prefab/modules/cxx/libs/android.arm64-v8a/libcxx.a"
    INTERFACE_INCLUDE_DIRECTORIES "D:/Java/.gradle/caches/9.4.1/transforms/2f236ca728f23a1fe2e62708852f7d1d/transformed/libcxx-29.0.14206865/prefab/modules/cxx/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

