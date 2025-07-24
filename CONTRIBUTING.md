# Contributing to FixupXer

Thank you for your interest in contributing to FixupXer! This document outlines the process and requirements for contributing to the project.

## 📋 Prerequisites

- **Licensing Agreement**: All contributions must be made under the GNU General Public License v3.0 or later (GPL-3.0-or-later)
- **Developer Certificate of Origin**: All commits must include a valid DCO sign-off
- **Code Quality**: Follow the existing code style and include appropriate tests

## 🔏 Developer Certificate of Origin (DCO)

By contributing to this project, you certify that:

1. The contribution was created in whole or in part by you and you have the right to submit it under the open source license indicated in the file; or
2. The contribution is based upon previous work that, to the best of your knowledge, is covered under an appropriate open source license and you have the right under that license to submit that work with modifications, whether created in whole or in part by you, under the same open source license (unless you are permitted to submit under a different license), as indicated in the file; or
3. The contribution was provided directly to you by some other person who certified (1), (2) or (3) and you have not modified it.
4. You understand and agree that this project and the contribution are public and that a record of the contribution (including all personal information you submit with it, including your sign-off) is maintained indefinitely and may be redistributed consistent with this project or the open source license(s) involved.

## ✍️ Signing Your Commits

Every commit **must** include a `Signed-off-by` line. You can do this by:

### Option 1: Use the -s flag (Recommended)
```bash
git commit -s -m "Your commit message"
```

### Option 2: Add manually
Add this line to your commit message:
```
Signed-off-by: Your Name <your.email@example.com>
```

### Option 3: Configure automatic signing
```bash
git config user.name "Your Name"
git config user.email "your.email@example.com"
git config alias.cs "commit -s"
```

Then use `git cs` instead of `git commit`.

## 🚀 Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/yourusername/fixupxer.git
   cd fixupxer
   ```

3. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

4. **Make your changes** and ensure they follow our coding standards

5. **Test your changes**:
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```

6. **Commit with DCO sign-off**:
   ```bash
   git commit -s -m "Add feature: your description"
   ```

7. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

8. **Create a Pull Request** on GitHub

## 📝 Coding Standards

- **Language**: Kotlin for Android development
- **Architecture**: Follow MVVM pattern with Repository pattern
- **Dependencies**: Use dependency injection with Hilt
- **Testing**: Write unit tests for business logic, UI tests for user interactions
- **Documentation**: Add KDoc comments for public APIs
- **License Headers**: All new files must include GPL-3.0-or-later headers

### Code Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions small and focused
- Maximum line length: 120 characters

## 🧪 Testing Requirements

- **Unit Tests**: Required for new business logic
- **Integration Tests**: Required for repository and database changes
- **UI Tests**: Required for new user-facing features
- **Performance Tests**: Required for URL processing changes

Run tests with:
```bash
# Unit tests
./gradlew test

# Integration tests  
./gradlew testDebugUnitTest

# UI tests (requires emulator)
./gradlew connectedAndroidTest
```

## 📦 Building the Project

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore)
./gradlew assembleRelease
```

## 🐛 Reporting Issues

- **Bug Reports**: Use the issue template provided
- **Feature Requests**: Discuss in discussions first
- **Security Issues**: Email neatcodelabs@gmail.com privately

## 📄 License

By contributing to FixupXer, you agree that your contributions will be licensed under the GNU General Public License v3.0 or later (GPL-3.0-or-later). You retain copyright to your contributions but grant us the right to distribute them under this license.

## ❓ Questions?

- **General Questions**: Open a discussion on GitHub
- **Technical Issues**: Open an issue with the bug template
- **Direct Contact**: Email neatcodelabs@gmail.com

---

**Important**: Pull requests without valid DCO sign-offs will be automatically rejected by our CI system.

Thank you for contributing to a cleaner, more private internet! 🌐✨
