# My assignment

## The solution
The solution consist of 3 main classes: `TransferController`, `TransferService`, `TransferRepositoryInMemory`.
* `TransferController` - a Spring REST controller responsible for handling incoming HTTP POST requests to initiate money transfers. It receives and validates a `TransferRequest` object containing details of the transfer, such as source and destination account IDs and the transfer amount. It then delegates the transfer request to the `TransferService`, logs the request, and returns a response containing a `TransferResponse`.
* `TransferService` -  class facilitates money transfers between accounts. It validates transfer input parameters and checks for sufficient account balances before executing a transfer through the `transferRepository`. After a successful transfer, it logs the transaction, notifies both the sender and receiver about the transfer, and returns a transfer response. If the transfer cannot be executed, it handles exceptions and provides appropriate error responses.
* `TransferRepositoryInMemory` - class is responsible for managing and facilitating money transfers between accounts in a multi-threaded environment. It uses a `ConcurrentHashMap` to store records of transfers. Additionally, it employs `ReentrantLocks` to prevent potential deadlocks when transferring funds between accounts by acquiring and releasing locks in a controlled order.


## Notes:
The repository contains 2 commits:
* `initial commit` - the version of the application given to me
* `dws challenge` - the solution for the given task
