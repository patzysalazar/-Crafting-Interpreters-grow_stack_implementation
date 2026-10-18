//> Chunks of Bytecode chunk-c
#include <stdlib.h>

#include "chunk.h"
//> chunk-c-include-memory
#include "memory.h"
//< chunk-c-include-memory
//> Garbage Collection chunk-include-vm
#include "vm.h"
//< Garbage Collection chunk-include-vm

void initChunk(Chunk* chunk) {
  chunk->count = 0;
  chunk->capacity = 0;
  chunk->code = NULL;
//> chunk-null-lines
  chunk->lines = NULL;
  chunk->lineCount = 0;
  chunk->lineCapacity = 0;
//< chunk-null-lines
//> chunk-init-constant-array
  initValueArray(&chunk->constants);
//< chunk-init-constant-array
}
//> free-chunk
void freeChunk(Chunk* chunk) {
  FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
//> chunk-free-lines
  FREE_ARRAY(Group, chunk->lines, chunk->lineCapacity);
//< chunk-free-lines
//> chunk-free-constants
  freeValueArray(&chunk->constants);
//< chunk-free-constants
  initChunk(chunk);
}
//< free-chunk
/* Chunks of Bytecode write-chunk < Chunks of Bytecode write-chunk-with-line
void writeChunk(Chunk* chunk, uint8_t byte) {
*/
//> write-chunk
//> write-chunk-with-line
void writeChunk(Chunk* chunk, uint8_t byte, int line) {
//< write-chunk-with-line
  if (chunk->capacity < chunk->count + 1) {
    int oldCapacity = chunk->capacity;
    chunk->capacity = GROW_CAPACITY(oldCapacity);
    chunk->code = GROW_ARRAY(uint8_t, chunk->code,
        oldCapacity, chunk->capacity);
//> write-chunk-line
    //chunk->lines = GROW_ARRAY(int, chunk->lines,
        //oldCapacity, chunk->capacity);
//< write-chunk-line
  }
  if (chunk->lineCapacity == chunk->lineCount) {//increase the line capacity if the line count is at capacity
      int oldCapacity = chunk->lineCapacity;
      chunk->lineCapacity = GROW_CAPACITY(oldCapacity);
      chunk->lines = GROW_ARRAY(Group, chunk->lines,
          oldCapacity, chunk->lineCapacity);
  }

//if first isntruction - create new group
  if (chunk-> lineCount == 0) {//if no linecount its a first group
      chunk->lines[chunk->lineCount].sourceLineNumb = line;//stores source line numb index 0
      chunk->lines[chunk->lineCount].instructionCount = 1;
      chunk->lineCount++;

   }
   else if(chunk->lines[chunk->lineCount - 1].sourceLineNumb == line){//else-if instruction line same as previous line- add to group
      chunk->lines[chunk->lineCount-1].instructionCount++;
   }
   else{//else- create new group, make sure instruction is set to 1 for starting intruction
      chunk->lines[chunk->lineCount].sourceLineNumb = line;//stores source line numb index 0
      chunk->lines[chunk->lineCount].instructionCount = 1;
      chunk->lineCount++;

   }

  chunk->code[chunk->count] = byte;
//> chunk-write-line
  //chunk->lines[chunk->count] = line;
//< chunk-write-line
  chunk->count++;
}


//which line instruction came from
int getLine(Chunk* chunk, int instruct){//given the index of an instruction, determines the line where the instruction occurs.
     int start = 0;//starting instruction index of current group
     int end = 0;//where current group ends
     int indexG = 0;//group index, which group we are checking

     while(indexG < chunk->lineCount){//checks if there are groups
        end = start + chunk-> lines[indexG].instructionCount;//finding where current group of instruction ends
        if(instruct < end){//if instruction in group return sourcelinenumb
            return chunk->lines[indexG].sourceLineNumb;
        }
          else{
            start = end;//sets starting index to where next group begins to look at correct index in next group
            indexG++;//next group index
        }

     }
     return -1;//if no match found return this
     }

//< write-chunk
//> add-constant
int addConstant(Chunk* chunk, Value value) {
//> Garbage Collection add-constant-push
  push(value);
//< Garbage Collection add-constant-push
  writeValueArray(&chunk->constants, value);
//> Garbage Collection add-constant-pop
  pop();
//< Garbage Collection add-constant-pop
  return chunk->constants.count - 1;
}
//< add-constant
void writeConstant(Chunk* chunk, Value value, int line){
    int index = addConstant(chunk, value);
    if(index>= 256){//index can't fit in one byte
        writeChunk(chunk, OP_CONSTANT_LONG, line);//opcode
        writeChunk(chunk, (uint8_t)(index & 0xff), line);//index byte 1
        writeChunk(chunk, (uint8_t)((index>>8 & 0xff)), line);//byte 2
        writeChunk(chunk, (uint8_t)((index>>16 & 0xff)), line);//byte 3
    }
    else{//index from 0-255
        writeChunk(chunk, OP_CONSTANT, line);//opcode
        writeChunk(chunk, (uint8_t)index, line);//index byte 1
    }

}