$LOAD_PATH.unshift(File.join(File.dirname(__FILE__), "..", "..", "lib"))

require "java"
require "multiverse"

class Philosopher
  def initialize(name, chopstick_left, chopstick_right, eat_time, think_time)
    @name = name
    @left_stick = chopstick_left
    @right_stick = chopstick_right
    @think_time = think_time
    @eat_time = eat_time
    @state = Ref()
    @state.set "thinking"
  end
  def think
    atomic {
      if @state.get.eql? "eating" 
        @left_stick.release
        @right_stick.release
      end
      @state.set "thinking"
    }
    puts "#{@name} thinking now"
    sleep @think_time
  end
  def eat
    atomic {
      @left_stick.pickup
      @right_stick.pickup
      @state.set "eating"
    }
    puts "#{@name} eating now"
    sleep @eat_time
  end
  def live
    20.times do |i|
      puts "#{i} time for #{@name}"
      think
      eat
      think
    end
  end
end

class ChopStick
  def initialize
    @state = Ref()
    @state.set "down"
  end
  def pickup
    atomic {
      Retry() if @state.get.eql? "up"
      @state.set "up"
    }
  end
  def release
    @state.set "down"
  end
end


c12 = ChopStick.new
c23 = ChopStick.new
c34 = ChopStick.new
c45 = ChopStick.new
c51 = ChopStick.new

godel = Philosopher.new("godel", c12, c51, 0.2, 0.1)
nietzsche = Philosopher.new("nietzche", c12, c23, 0.2, 0.1)
hume = Philosopher.new("hume", c23, c34, 0.1, 0.2)
aristotle = Philosopher.new("aristotle", c34, c45, 0.1, 0.2)
plato = Philosopher.new("plato", c45, c51, 0.2, 0.1) 

threads = []
[godel, nietzsche, hume, aristotle, plato].each do |p|
   threads << Thread.new { p.live }
end

threads.each {|t| t.join }
puts "Philosophers disperse"
