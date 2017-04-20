package io.mifos.accounting.service.internal.command;

public class DeleteAccountCommand {
  final String identifier;

  public DeleteAccountCommand(final String identifier) {
    super();
    this.identifier = identifier;
  }

  public String identifier() {
    return this.identifier;
  }
}
