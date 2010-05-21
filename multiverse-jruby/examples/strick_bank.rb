require "java"
require "multiverse"
require "test/unit/assertions"
include Test::Unit::Assertions

class NotEnoughFundException < RuntimeError
  def initialize(message)
    super(message)
  end
end

class Account
  def initialize(amount)
    @balance = Ref()
    @balance.set(amount) 
  end
  def get_balance
    return @balance.get()
  end
  def debit(amount)
    atomic {
      raise NotEnoughFundException.new("Unable to transfer #{amount}") if(amount > @balance.get())
      @balance.set(@balance.get() - amount)
    }
  end
  def credit(amount)
    atomic {
      @balance.set(@balance.get + amount)
    }
  end
end

def transfer(from, to, amount)
  atomic {
    to.credit(amount)
    from.debit(amount)
  }
end

sai = Account.new(10)
hui = Account.new(20)

assert_equal sai.get_balance, 10, "Sai's Initial balance"
assert_equal hui.get_balance, 20, "Hui's Initial balance"

puts "Initial - Sai's balance #{sai.get_balance}"
puts "Initial - Hui's balance #{hui.get_balance}"

th = []

th << Thread.new do
  begin
    transfer(sai, hui, 15)
  rescue RuntimeError
    puts "Unable to transfer 15 from Sai to Hui"
  end
  puts "Thread transfering from me to hui"
  puts "Thread1 - Sai's balance #{sai.get_balance}"
  puts "Thread1 - Hui's balance #{hui.get_balance}"
end

th.each {|t| t.join }

puts "Final - Sai's balance #{sai.get_balance}"
puts "Final - Hui's balance #{hui.get_balance}"

assert_equal sai.get_balance, 10, "Sai's Final balance"
assert_equal hui.get_balance, 20, "Hui's Final balance"
