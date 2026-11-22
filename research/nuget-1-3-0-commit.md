# Which Dotmim.Sync commit matches NuGet 1.3.0?

**Answer:** `856998049e205728924546c29e363340b6211494` (short `8569980`), on the `master` branch of Mimetis/Dotmim.Sync. There is no git tag for 1.3.0.

README version table entry: **1.3.0 (commit 8569980, no git tag)**

## Evidence

- The nuspec in each published nupkg (Dotmim.Sync.Core, Dotmim.Sync.Sqlite and Dotmim.Sync.Web.Client 1.3.0, from `https://api.nuget.org/v3-flatcontainer/<id>/1.3.0/<id>.1.3.0.nupkg`) has the same line:
  `<repository type="git" url="https://github.com/Mimetis/Dotmim.Sync" branch="master" commit="856998049e205728924546c29e363340b6211494" />`
- In the local C# clone, `8569980` is "split pipelines", dated 2025-03-12. Its Directory.Build.props sets `<Version>1.3.0</Version>`.
- The version bump commit `bcc5148` ("version 1.3.0", 2024-09-25, found with `git log -S`) is an ancestor of `8569980`. So 1.3.0 was published about six months after the bump, from a later master commit, not from the bump commit.
- `git tag` has no 1.3.0 tag. The latest tags are 1.0.2 and v0.9.6.
