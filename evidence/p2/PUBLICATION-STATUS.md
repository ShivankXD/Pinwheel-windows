# P2 publication recovery

Resolved on 2026-10-03: a buffered HTTP/1.1 fast-forward push published the
exact local commits `13d23cba6e0fcb2b785c912215ce5d0b39606bb3` and
`1b7c8a388277bb49eaef3f9b91e84144e8f98c7f`. Local and remote main were
verified at 1b7c8a3. No history was rewritten and no Git Data API tree, commit
or reference was created. GitHub lists only ShivankXD, with 25 contributions
at this checkpoint.

Hosted Windows/JDK 17 [run 37097525085](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/37097525085)
completed successfully for exact runtime 1b7c8a3. All 15 steps pass; the
artifact is `p2-evidence`, id 11264907345. Owner golden comparisons remain
local and failing. The crop-filter correction was committed and pushed as
`15adf3875456b68eb64de36b047bf909dd91ed99`; all 237 local tests pass and
production pixels are unchanged. Its hosted Windows/JDK 17
[run 37098985024](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/37098985024)
also passes all 15 steps; artifact id 11265088093. `ci-result.json` identifies
the exact tested runtime. The alternative 8-bit controls were committed and
pushed as `6ac275330aa64a9028024ff9929a47e54094db48`; hosted
[run 37099505455](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/37099505455)
passes all 15 steps, artifact id 11264874701. This task compiles in CI; its
224 owner-reference comparisons run locally. The CI record now names this
tested commit. Publication is recovered, with P2 visual acceptance still open.

## Historical transport failures, 2026-10-02

At that time, the validated deterministic and sampling controls were saved in local commit
`13d23cba6e0fcb2b785c912215ce5d0b39606bb3`. GitHub main was verified at
`df75ced84493b0f361d3488ad4cb7652d205028e` after the failed attempts. The new
runtime had not been published and had no hosted CI result. The then-current
`ci-result.json` named the previously tested runtime.

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

The successful recovery retained those exact validated commits, verified the
remote SHA and obtained the new hosted result before updating CI evidence.
The historical failures above do not describe the current publication state.
