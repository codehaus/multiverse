require "test_helper"

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
      Retry() if(amount > @balance.get())
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
    from.debit(amount)
    to.credit(amount)
  }
end

sai = Account.new(10)
hui = Account.new(20)
peter = Account.new(15)

assert_equal sai.get_balance, 10, "Sai's Initial balance"
assert_equal hui.get_balance, 20, "Hui's Initial balance"
assert_equal peter.get_balance, 15, "Peter's Initial balance"

puts "Initial - Sai's balance #{sai.get_balance}"
puts "Initial - Hui's balance #{hui.get_balance}"
puts "Initial - Peters's balance #{peter.get_balance}"
th = []

th << Thread.new do
  transfer(sai, hui, 15)
  puts "Thread transfering from me to hui"
  puts "Thread1 - Sai's balance #{sai.get_balance}"
  puts "Thread1 - Hui's balance #{hui.get_balance}"
  puts "Thread1 - Peters's balance #{peter.get_balance}"
end

th << Thread.new do
  sleep(0.5)
  transfer(peter, sai, 5)
  puts "Thread transfering from peter to me"
  puts "Thread2 - Sai's balance #{sai.get_balance}"
  puts "Thread2 - Hui's balance #{hui.get_balance}"
  puts "Thread2 - Peters's balance #{peter.get_balance}"
end

th.each {|t| t.join }

puts "Final - Sai's balance #{sai.get_balance}"
puts "Final - Hui's balance #{hui.get_balance}"
puts "Final - Peters's balance #{peter.get_balance}"

assert_equal sai.get_balance, 0, "Sai's Final balance"
assert_equal hui.get_balance, 35, "Hui's Final balance"
assert_equal peter.get_balance, 10, "Peter's Final balance"
