# P2 publication blocker

The validated deterministic and sampling controls are saved in local commit
`13d23cba6e0fcb2b785c912215ce5d0b39606bb3`. GitHub main was verified at
`df75ced84493b0f361d3488ad4cb7652d205028e` after the failed attempts. The new
runtime has not been published and has no hosted CI result. `ci-result.json`
continues to name the previously tested runtime, never the unpublished one.

The initial default HTTPS upload made no observable progress for several
minutes and was stopped. Buffered uploads using HTTP/1.1 and HTTP/2, an
explicit low-speed timeout, and Windows native TLS all failed. No Git
configuration was changed globally. These were normal fast-forward pushes;
no published history was rewritten.

Complete terminal error messages for the stopped initial attempt:

```text
send-pack: unexpected disconnect while reading sideband packet
error: failed to push some refs to 'https://github.com/ShivankXD/Pinwheel-windows.git'
```

Complete terminal error messages for HTTP/1.1, buffered HTTP/1.1 and native TLS:

```text
error: RPC failed; HTTP 408 curl 22 The requested URL returned error: 408
send-pack: unexpected disconnect while reading sideband packet
fatal: the remote end hung up unexpectedly
Everything up-to-date
```

Complete terminal error messages for buffered HTTP/2:

```text
error: RPC failed; HTTP 408 curl 55 Recv failure: Connection was reset
send-pack: unexpected disconnect while reading sideband packet
fatal: the remote end hung up unexpectedly
Everything up-to-date
```

The final `Everything up-to-date` line in these failing outputs does not prove
publication. Each command exited nonzero and the remote branch SHA remained
unchanged. The pending pack contains 423 objects and is 15.08 MiB.

A Git Data API recovery used the existing Git credential only in memory.
Individual objects were checked against their local SHA before recording
success. Some uploads succeeded, but larger requests repeatedly timed out,
disconnected during TLS writes, or returned HTTP 400. Smaller streaming
writes and lower concurrency did not complete the upload. A compressed
request experiment was rejected as JSON. Complete server error messages:

```text
GitHub API POST /repos/ShivankXD/Pinwheel-windows/git/blobs: HTTP 400
We received a malformed request from your client. Sorry about that. Please try resubmitting your request and contact us if the problem persists.
Problems parsing JSON
ssl.SSLEOFError: EOF occurred in violation of protocol (_ssl.c:2406)
API transport timeout: POST /repos/ShivankXD/Pinwheel-windows/git/blobs
```

The API never reached tree creation, commit creation or branch update. Its
successful unreferenced blob uploads do not constitute publication. The local
recovery state contains only object hashes, with no credentials. No public
branch, tag or partial checkpoint was created. GitHub's contributor list still
contains only ShivankXD.

Local validation remains complete: 236 unit tests, four mobile package checks,
56 exact stored-input replays, all 1688 unchanged production frame hashes,
and source/reference audits pass. Golden acceptance still fails with 18
deterministic frames and 84 noise structural frames. No threshold or shader
was changed to make this checkpoint pass.

The next publication attempt must retain the exact validated local commits,
verify the remote SHA, and obtain a new hosted CI result before updating the
CI evidence. This report does not claim that the owner-requested push succeeded.
