# Jobcoin Mixer

To start the mixer server, run:
```
sbt +run
```
Note the first compile takes a while due to a forked dependency being built from source.

Once the server has started, visit [http://localhost:8081/docs](http://localhost:8081/docs) to see the available endpoints and their documentation.

## Mixing workflow
The api for this mixer deviates slightly from the specification. To mix Jobcoins:
* Ask for a mixer deposit address using `/getDepositAddress`
* Deposit your coins into that address 
* Send the mixer that address, as long  with a list of addresses you own, and some mixing parameters:
  * `maxMixerFee` specifies the maximum allowable service fee. The actual fee taken will be a randomly generated number between 0 and the specified amount. This is to make it harder to map addresses based on transactions.
  * `maxTransactionSeconds` is the maximum amount of time the mixer will wait before making deposits into your account. The actual amount of time the mixer will wait is a randomly generated number of seconds between 0 and the specified maximum.

## Notes on the API change
This mixer associates the deposit address to withdrawal addresses only after funds have already been deposited. In a real system this would require the mixer to provide an authentication method associated with the deposit address it provides, which may be undesirable.

## Code breakdown
This codebase is split into two projects:
* `api`, which specifies the APIs of the jobcoin client and the mixer server
* `mixer`, which contains the implementation of the mixer api in `api`

## Running tests
To run the tests:
```
sbt +test
```

## Potential improvements
* Persist planned transactions (but be careful about keeping transactions logs). Currently there is no way to recover from a failure when depositing coins from the mixer account to the user accounts.
* More tests™
