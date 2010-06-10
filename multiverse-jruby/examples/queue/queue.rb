$LOAD_PATH.unshift(File.join(File.dirname(__FILE__), "..", "..", "lib"))

require "java"
require "multiverse"
require "pp"

class Queue
  def initialize(max_size)
    @queue = []
    @size = LongRef.new(0)
    @max_size = max_size
  end
  def wait_if_empty
    atomic {
    Retry() if @size.get.to_i == 0
    }
  end
  def value
    return @queue
  end
  def wait_if_full
    atomic {
    Retry() if @size.get.to_i == @max_size
    }
  end
  def dequeue
    atomic {
      wait_if_empty
      result =  @queue.pop 
      @size.dec
      pp "Result is #{result}"
    }
  end
  def enqueue val
    atomic {
      wait_if_full
      puts "Value to be added #{val}"
      @queue = [val] + @queue
      @size.inc
      pp "The queue now is [#{@queue.join(", ")}]"
    }
  end
end


q = Queue.new(3)
t = []
t << Thread.new { q.enqueue 1 }
t << Thread.new { q.enqueue 2 }
t << Thread.new { q.enqueue 3 }
t << Thread.new { sleep 1;  q.dequeue }
t << Thread.new { q.enqueue 4 }


t.each {|thread| thread.join }
pp q.value
