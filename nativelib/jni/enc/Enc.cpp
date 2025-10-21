//
// Created by Lythe on 2025/9/9.
//
#include <jni.h>
#include <cstdio>
#include <android/log.h>
#include <cstdlib>
#include <cstring>
#include <unistd.h>
#include <sys/types.h>
#include <elf.h>
#include <sys/mman.h>

#define LOG_TAG "Enc"
#define LOGI(...) __android_log_print( \
ANDROID_LOG_INFO,                      \
LOG_TAG,                               \
__VA_ARGS__)

u_long getLibAddr();
extern "C" {
    __attribute__((section(".mytext"), used)) jstring getString(JNIEnv* env)
    {
        return (*env).NewStringUTF("This is native method");
    };
}

__attribute__((constructor())) void init_getString()
{
    char name[15];
    uint nblock;
    uint nsize;
    u_long base;
    u_long text_addr;
    uint i;
    Elf64_Ehdr *ehdr;
    Elf64_Shdr *shdr;

    base = getLibAddr();

    ehdr = (Elf64_Ehdr*)base;
    text_addr = ehdr->e_shoff + base;

    nblock = ehdr->e_entry >> 16;
    nsize = ehdr->e_entry & 0xffff;

    LOGI("nblock =  0x%x,nsize:%d", nblock, nsize);
    LOGI("base =  0x%lx", text_addr);
    printf("nblock = %d\n", nblock);
    if(mprotect((void *) (text_addr / PAGE_SIZE * PAGE_SIZE), 4096 * nsize, PROT_READ | PROT_EXEC | PROT_WRITE) != 0)
    {
        puts("mem privilege change failed");
        LOGI("mem privilege change failed");
    }

    for(i = 0; i < nblock; i++)
    {
        char* addr = (char*)(text_addr + i);
        *addr = ~(*addr);
    }
    if(mprotect((void *) (text_addr / PAGE_SIZE * PAGE_SIZE), 4096 * nsize, PROT_READ | PROT_EXEC) != 0)
    {
        puts("mem privilege change failed");
    }
    puts("Decrypt success");
}

u_long getLibAddr()
{
    u_long ret = 0;
    char name[] = "libplayer.so";
    char buf[4096], *temp;
    int pid;
    FILE* fp;
    pid = getpid();
    sprintf(buf, "/proc/%d/maps", pid);
    fp = fopen(buf, "r");
    if(fp == nullptr)
    {
        puts("open failed");
        goto _error;
    }
    while(fgets(buf, sizeof(buf), fp))
    {
        if(strstr(buf, name))
        {
            temp = strtok(buf, "-");
            ret = strtoul(temp, nullptr, 16);
            break;
        }
    }

    _error:
    fclose(fp);
    return ret;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_lythe_nativelib_MainActivity_test(JNIEnv *env, jobject thiz) {
    // TODO: implement test()
    return (*env).NewStringUTF("Test");
}