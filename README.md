# Quincy

Quincy is an implementation of [QUIC](https://quicwg.org/) in Java, based 
on the Netty framework. Focusing on having fun and learning. Development 
is still very much in an early stage, exploring what an implementation 
could look like. That is, for now the priorities are Complete > Correct > Performant.

We're more than happy to accept contributions, feel free to take a stab at 
anything missing, incomplete or wrong.

- [ ] TLS (custom implementation)
  - [X] Messages/extensions
  - [X] Handshake
  - [ ] Certification validation
  - [ ] Key phase
  - [ ] ALPN
- [X] Protocol/packets/frames
- [X] Connections
- [ ] Packet coalescing
- [ ] PMTU
- [X] Version negotiation
- [X] Streams
- [ ] Reliability/resends/ack
- [ ] Flow control
  - [X] Max data
  - [ ] Max streams
- [ ] Congestion control
- [ ] Address validation
  - [X] Retry
  - [ ] Path validation
- [ ] Connection migration
- [ ] Connection termination
  - [ ] Idle timeout
  - [X] Immediate close
  - [ ] Stateless reset
- [ ] HTTP binding
- [X] Integration tests (quic-go, quicly)
