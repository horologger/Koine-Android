package com.koine.mesh.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koine.mesh.repository.SatochipRepository
import com.koine.mesh.satochip.client.Constants
import com.koine.mesh.satochip.client.SatochipParser
import com.koine.mesh.satochip.client.CardChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SatochipViewModel @Inject constructor(
    private val satochipRepository: SatochipRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<SatochipUiState>(SatochipUiState.Initial)
    val uiState: StateFlow<SatochipUiState> = _uiState.asStateFlow()
    
    fun initializeCard(cardChannel: CardChannel) {
        viewModelScope.launch {
            _uiState.value = SatochipUiState.Loading
            val success = satochipRepository.initializeCard(cardChannel)
            if (success) {
                val status = satochipRepository.getStatus()
                _uiState.value = if (status != null) {
                    SatochipUiState.Success(status)
                } else {
                    SatochipUiState.Error("Failed to get card status")
                }
            } else {
                _uiState.value = SatochipUiState.Error("Failed to initialize card")
            }
        }
    }
    
    fun verifyPin(pin: String) {
        viewModelScope.launch {
            _uiState.value = SatochipUiState.Loading
            val success = satochipRepository.verifyPin(pin)
            if (success) {
                val status = satochipRepository.getStatus()
                _uiState.value = if (status != null) {
                    SatochipUiState.Success(status)
                } else {
                    SatochipUiState.Error("Failed to get card status")
                }
            } else {
                _uiState.value = SatochipUiState.Error("Invalid PIN")
            }
        }
    }
    
    fun changePin(oldPin: String, newPin: String) {
        viewModelScope.launch {
            _uiState.value = SatochipUiState.Loading
            val success = satochipRepository.changePin(oldPin, newPin)
            if (success) {
                val status = satochipRepository.getStatus()
                _uiState.value = if (status != null) {
                    SatochipUiState.Success(status)
                } else {
                    SatochipUiState.Error("Failed to get card status")
                }
            } else {
                _uiState.value = SatochipUiState.Error("Failed to change PIN")
            }
        }
    }
    
    fun signData(data: ByteArray) {
        viewModelScope.launch {
            _uiState.value = SatochipUiState.Loading
            val signature = satochipRepository.sign(data)
            if (signature != null) {
                _uiState.value = SatochipUiState.SignatureSuccess(signature)
            } else {
                _uiState.value = SatochipUiState.Error("Failed to sign data")
            }
        }
    }
    
    fun getPublicKey(keyType: Byte) {
        viewModelScope.launch {
            _uiState.value = SatochipUiState.Loading
            val publicKey = satochipRepository.getPublicKey(keyType)
            if (publicKey != null) {
                _uiState.value = SatochipUiState.PublicKeySuccess(publicKey)
            } else {
                _uiState.value = SatochipUiState.Error("Failed to get public key")
            }
        }
    }
}

sealed class SatochipUiState {
    object Initial : SatochipUiState()
    object Loading : SatochipUiState()
    data class Success(val status: SatochipParser.ApplicationStatus) : SatochipUiState()
    data class SignatureSuccess(val signature: ByteArray) : SatochipUiState()
    data class PublicKeySuccess(val publicKey: ByteArray) : SatochipUiState()
    data class Error(val message: String) : SatochipUiState()
} 