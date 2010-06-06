$LOAD_PATH.unshift(File.join(File.dirname(__FILE__), "..", "..", "lib"))

require "java"
require "multiverse"

class Dequeue
  def initialize(max_size)
    @queue = []
    @max_size = max_size
  end
  def is_empty?
    atomic {
      return @queue.empty?
    }
  end
  def is_full?
    atomic {
      return @queue.size.eql? @max_size
    }
  end
  def pop
    atomic {
      Retry() if is_empty?
      return @queue.pop 
    }
  end
  def push val
    atomic {
      Retry() if is_full?
      @queue = [val] + @queue
      puts @queue
    }
  end
end


q = Dequeue.new(3)
t = []
t << Thread.new { q.push 1 }
t << Thread.new { q.push 2 }
t << Thread.new { q.push 3 }
t << Thread.new { sleep 0.2; puts q.pop }
t << Thread.new { q.push 4 }

t.each {|thread| thread.join }
